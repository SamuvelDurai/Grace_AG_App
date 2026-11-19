package com.example.data.content

import android.content.Context
import com.example.core.model.BibleVerse
import com.example.core.model.DailyVerseConfig
import com.example.core.util.FirestoreProvider
import com.example.core.util.OperationType
import com.example.core.util.handleFirestoreError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

enum class DailyVerseSource {
  PASTOR_PINNED_TODAY,
  PASTOR_CUSTOM,
  AUTO_DAILY
}

data class DailyVerseResolution(
  val verse: BibleVerse,
  val source: DailyVerseSource,
  val rotationPool: List<BibleVerse>
)

class BibleContentRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(FirestoreProvider.getInstance(context))

  private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

  private val customVerses = mutableListOf<BibleVerse>()
  private val _versesFlow = MutableStateFlow<List<BibleVerse>>(emptyList())
  private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
  private val _dailyVerseConfigFlow = MutableStateFlow(DailyVerseConfig())

  fun observeFavoriteVerseIds(): Flow<Set<String>> = _favoriteIds.asStateFlow()

  fun observeDailyVerses(churchId: String = "church_main"): Flow<List<BibleVerse>> {
    if (auth.currentUser == null) {
      return combine(_versesFlow.asStateFlow(), _favoriteIds) { list, favs ->
        list.map { verse -> verse.copy(isFavorite = verse.isFavorite || favs.contains(verse.verseId)) }
      }
    }

    val remoteFlow = db.collection("bibleVerses")
      .whereEqualTo("churchId", churchId)
      .limit(50)
      .snapshots()
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.LIST, "bibleVerses")
      }

    return combine(remoteFlow, _favoriteIds) { snapshot, favs ->
      snapshot.toObjects(BibleVerse::class.java)
        .sortedByDescending { it.createdAt?.seconds ?: 0L }
        .map { verse ->
          verse.copy(isFavorite = verse.isFavorite || favs.contains(verse.verseId))
        }
    }.catch { error ->
      if (error is CancellationException) throw error
      handleFirestoreError(error, OperationType.LIST, "bibleVerses")
      emit(emptyList())
    }
  }

  fun observeDailyVerseConfig(churchId: String = "church_main"): Flow<DailyVerseConfig> {
    if (auth.currentUser == null) {
      return _dailyVerseConfigFlow.asStateFlow()
    }

    val docRef = db.collection("dailyVerseConfig").document(churchId)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(DailyVerseConfig::class.java)
            ?.copy(churchId = churchId)
            ?: DailyVerseConfig(churchId = churchId)
        } else {
          DailyVerseConfig(churchId = churchId, autoDailyEnabled = true)
        }
      }
      .catch { error ->
        if (error is CancellationException) throw error
        handleFirestoreError(error, OperationType.GET, docRef.path)
        emit(DailyVerseConfig(churchId = churchId, autoDailyEnabled = true))
      }
  }

  suspend fun setAutoDailyVerseEnabled(
    enabled: Boolean,
    pastorName: String,
    churchId: String = "church_main"
  ): Result<DailyVerseConfig> = runCatching {
    val cleanChurchId = churchId.ifBlank { "church_main" }
    val currentUser = auth.currentUser
    val config = DailyVerseConfig(
      churchId = cleanChurchId,
      autoDailyEnabled = enabled,
      rotationHour = 6,
      updatedByName = pastorName.trim().ifBlank { "Pastor" },
      updatedByUid = currentUser?.uid ?: "local_pastor"
    )
    _dailyVerseConfigFlow.value = config

    if (currentUser != null) {
      val docRef = db.collection("dailyVerseConfig").document(cleanChurchId)
      val payload = hashMapOf<String, Any?>(
        "churchId" to cleanChurchId,
        "autoDailyEnabled" to enabled,
        "rotationHour" to 6,
        "updatedByName" to config.updatedByName,
        "updatedByUid" to currentUser.uid,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.set(payload, SetOptions.merge()).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, docRef.path)
        throw e
      }
    }
    config
  }

  fun toggleFavorite(verseId: String) {
    val currentFavs = _favoriteIds.value.toMutableSet()
    if (!currentFavs.add(verseId)) {
      currentFavs.remove(verseId)
    }
    _favoriteIds.value = currentFavs

    val idx = customVerses.indexOfFirst { it.verseId == verseId }
    if (idx != -1) {
      val item = customVerses[idx]
      customVerses[idx] = item.copy(isFavorite = !item.isFavorite)
      _versesFlow.value = customVerses.toList()
    }
  }

  suspend fun publishVerse(
    reference: String,
    textEn: String,
    textTa: String,
    devEn: String,
    devTa: String,
    pastorName: String,
    pinForToday: Boolean = false,
    churchId: String = "church_main"
  ): Result<BibleVerse> = runCatching {
    val cleanRef = reference.trim()
    val cleanEn = textEn.trim()
    val cleanTa = textTa.trim()
    require(cleanRef.isNotBlank()) { "Bible book & verse reference is required." }
    require(cleanEn.isNotBlank() || cleanTa.isNotBlank()) { "Please enter verse text in English or Tamil." }

    val resolvedEn = cleanEn.ifBlank { cleanTa }
    val resolvedTa = cleanTa.ifBlank { cleanEn }
    val pinKey = if (pinForToday) todayDateKey() else ""
    val newVerse = BibleVerse(
      verseId = "verse_${UUID.randomUUID().toString().take(12)}",
      churchId = churchId.ifBlank { "church_main" },
      bookReference = cleanRef,
      textEnglish = resolvedEn,
      textTamil = resolvedTa,
      devotionalEnglish = devEn.trim(),
      devotionalTamil = devTa.trim(),
      scheduledDate = if (pinForToday) "Pinned Today ($pinKey)" else "Pastor Collection",
      pinDateKey = pinKey,
      publishedByPastor = pastorName.trim().ifBlank { "Senior Pastor" }
    )
    customVerses.add(0, newVerse)
    _versesFlow.value = customVerses.toList()

    if (auth.currentUser != null) {
      val docRef = db.collection("bibleVerses").document(newVerse.verseId)
      val payload = hashMapOf<String, Any?>(
        "verseId" to newVerse.verseId,
        "churchId" to newVerse.churchId,
        "bookReference" to newVerse.bookReference,
        "textEnglish" to newVerse.textEnglish,
        "textTamil" to newVerse.textTamil,
        "devotionalEnglish" to newVerse.devotionalEnglish,
        "devotionalTamil" to newVerse.devotionalTamil,
        "translationSource" to newVerse.translationSource,
        "scheduledDate" to newVerse.scheduledDate,
        "pinDateKey" to newVerse.pinDateKey,
        "publishedByPastor" to newVerse.publishedByPastor,
        "createdAt" to FieldValue.serverTimestamp()
      )
      try {
        docRef.set(payload).await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, docRef.path)
        throw e
      }
    }
    newVerse
  }

  suspend fun deleteVerse(verseId: String): Result<Unit> = runCatching {
    customVerses.removeAll { it.verseId == verseId }
    _versesFlow.value = customVerses.toList()

    if (auth.currentUser != null && verseId.isNotBlank()) {
      val docRef = db.collection("bibleVerses").document(verseId)
      try {
        docRef.delete().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, docRef.path)
        throw e
      }
    }
  }

  companion object {
    fun todayDateKey(calendar: Calendar = Calendar.getInstance()): String {
      return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    fun resolveActiveDailyVerse(
      customVerses: List<BibleVerse>,
      autoConfig: DailyVerseConfig = DailyVerseConfig(autoDailyEnabled = true),
      dayOfYear: Int = Calendar.getInstance().get(Calendar.DAY_OF_YEAR),
      todayKey: String = todayDateKey()
    ): DailyVerseResolution {
      val combinedPool = (customVerses + builtInDailyVerseCatalog)
        .distinctBy { it.bookReference.trim().lowercase() }

      val pinnedToday = customVerses.firstOrNull { it.pinDateKey == todayKey }
      if (pinnedToday != null) {
        return DailyVerseResolution(
          verse = pinnedToday,
          source = DailyVerseSource.PASTOR_PINNED_TODAY,
          rotationPool = combinedPool
        )
      }

      if (!autoConfig.autoDailyEnabled && customVerses.isNotEmpty()) {
        return DailyVerseResolution(
          verse = customVerses.first(),
          source = DailyVerseSource.PASTOR_CUSTOM,
          rotationPool = combinedPool
        )
      }

      val index = ((dayOfYear - 1).coerceAtLeast(0)) % combinedPool.size
      val selected = combinedPool[index]
      val source = if (autoConfig.autoDailyEnabled) {
        DailyVerseSource.AUTO_DAILY
      } else {
        DailyVerseSource.PASTOR_CUSTOM
      }
      return DailyVerseResolution(
        verse = selected,
        source = source,
        rotationPool = combinedPool
      )
    }

    val builtInDailyVerseCatalog: List<BibleVerse> = listOf(
      BibleVerse(
        verseId = "auto_01",
        bookReference = "Philippians 4:13",
        textEnglish = "I can do all things through Christ who strengthens me.",
        textTamil = "என்னைப் பலப்படுத்துகிற கிறிஸ்துவினாலே எல்லாவற்றையும் செய்ய எனக்குப் பெலனுண்டு.",
        devotionalEnglish = "In moments of weakness and challenge, Christ supplies quiet strength for every step today.",
        devotionalTamil = "நம்முடைய பெலவீனத்தின் மத்தியில் கர்த்தர் நமக்கு அளவற்ற கிருபையையும் பெலனையும் அருளுகிறார்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_02",
        bookReference = "Psalm 23:1",
        textEnglish = "The Lord is my shepherd; I shall not want.",
        textTamil = "கர்த்தர் என் மேய்ப்பராயிருக்கிறார்; நான் தாழ்ச்சியடையேன்.",
        devotionalEnglish = "Our Shepherd walks ahead of your family today, providing peace, direction, and grace.",
        devotionalTamil = "நம் நல்ல மேய்ப்பர் இன்றைய நாளில் உங்கள் குடும்பத்தை சமாதானத்தோடு நடத்துவார்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_03",
        bookReference = "Proverbs 3:5-6",
        textEnglish = "Trust in the Lord with all your heart, and lean not on your own understanding; in all your ways acknowledge Him, and He shall direct your paths.",
        textTamil = "உன் சுயபுத்தியின்மேல் சாயாமல், உன் முழு இருதயத்தோடும் கர்த்தரில் நம்பிக்கையாயிருந்து, உன் வழிகளிலெல்லாம் அவரை நினைத்துக்கொள்; அப்பொழுது அவர் உன் பாதைகளைச் செவ்வைப்படுத்துவார்.",
        devotionalEnglish = "Surrender today's decisions in prayer and watch God straighten the path before you.",
        devotionalTamil = "இன்றைய தீர்மானங்களை கர்த்தரிடம் ஒப்புவிக்க, அவர் உங்கள் வழிகளைச் செவ்வைப்படுத்துவார்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_04",
        bookReference = "Isaiah 41:10",
        textEnglish = "Fear not, for I am with you; be not dismayed, for I am your God. I will strengthen you and help you.",
        textTamil = "நீ பயப்படாதே, நான் உன்னுடனே இருக்கிறேன்; திகையாதே, நான் உன் தேவன்; நான் உன்னைப் பலப்படுத்தி உனக்குச் சகாயம்பண்ணுவேன்.",
        devotionalEnglish = "God's presence stays closer than any worry—step into today with calm courage.",
        devotionalTamil = "தேவனுடைய பிரசன்னம் எந்த பயத்தையும் விடப் பெரியது; தைரியத்தோடு இன்றைய நாளைத் தொடங்குங்கள்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_05",
        bookReference = "Jeremiah 29:11",
        textEnglish = "For I know the thoughts that I think toward you, says the Lord, thoughts of peace and not of evil, to give you a future and a hope.",
        textTamil = "நீங்கள் எதிர்பார்த்திருக்கும் முடிவை உங்களுக்குக் கொடுக்கும்படிக்கு நான் உங்கள்மேல் நினைத்திருக்கிற நினைவுகளை அறிவேன் என்று கர்த்தர் சொல்லுகிறார்; அவைகள் தீமைக்கல்ல, சமாதானத்துக்கேதுவான நினைவுகளே.",
        devotionalEnglish = "Even when plans shift, the Father holds your tomorrow in steadfast love and hope.",
        devotionalTamil = "எதிர்காலத்தைக் குறித்த நம்பிக்கையும் சமாதானமும் நம் பரம தகப்பனின் கரத்தில் உள்ளது.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_06",
        bookReference = "Psalm 46:1",
        textEnglish = "God is our refuge and strength, a very present help in trouble.",
        textTamil = "தேவன் நமக்கு அடைக்கலமும் பெலனும், ஆபத்துக்காலத்தில் அநுகூலமான துணையுமானவர்.",
        devotionalEnglish = "Whenever life feels overwhelming, turn first to God—He is an immediate, unshakeable shelter.",
        devotionalTamil = "நெருக்கடியான நேரத்தில் கர்த்தரே நமக்கு உடனடியான அடைக்கலமும் உறுதியான துணையுமாய் இருக்கிறார்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_07",
        bookReference = "Joshua 1:9",
        textEnglish = "Be strong and of good courage; do not be afraid, nor be dismayed, for the Lord your God is with you wherever you go.",
        textTamil = "பலங்கொண்டு திடமனதாயிரு; திகையாதே, கலங்காதே, நீ போகும் இடமெல்லாம் உன் தேவனாகிய கர்த்தர் உன்னோடே இருக்கிறார்.",
        devotionalEnglish = "You never walk into work, school, or ministry alone—His companionship goes with you.",
        devotionalTamil = "நீங்கள் செல்லும் ஒவ்வொரு இடத்திலும் தேவனுடைய கரம் உங்களோடு கூடச் செல்லும்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_08",
        bookReference = "Psalm 121:1-2",
        textEnglish = "I will lift up my eyes to the hills—from whence comes my help? My help comes from the Lord, who made heaven and earth.",
        textTamil = "எனக்கு ஒத்தாசை வரும் பர்வதங்களுக்கு நேராக என் கண்களை ஏறெடுக்கிறேன். வானத்தையும் பூமியையும் உண்டாக்கின கர்த்தரிடத்திலிருந்து எனக்கு ஒத்தாசை வரும்.",
        devotionalEnglish = "Look upward beyond present obstacles; help comes from the Creator of heaven and earth.",
        devotionalTamil = "சூழ்நிலைகளை அல்ல, வானத்தையும் பூமியையும் உண்டாக்கின கர்த்தரையே நம்பி நோக்கிப் பாருங்கள்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_09",
        bookReference = "Romans 8:28",
        textEnglish = "And we know that all things work together for good to those who love God, to those who are the called according to His purpose.",
        textTamil = "அன்றியும், அவருடைய தீர்மானத்தின்படி அழைக்கப்பட்டவர்களாய் தேவனிடத்தில் அன்புகூருகிறவர்களுக்குச் சகலமும் நன்மைக்கு ஏதுவாக நடக்கிறதென்று அறிந்திருக்கிறோம்.",
        devotionalEnglish = "Trust God to weave even delayed or difficult moments into a testimony of His goodness.",
        devotionalTamil = "நம் வாழ்வின் ஒவ்வொரு சூழ்நிலையையும் தேவன் நன்மைக்காகவே இணைத்து நடத்துகிறார்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_10",
        bookReference = "Matthew 11:28",
        textEnglish = "Come to Me, all you who labor and are heavy laden, and I will give you rest.",
        textTamil = "வருத்தப்பட்டுப் பாரஞ்சுமக்கிறவர்களே! நீங்கள் எல்லாரும் என்னிடத்தில் வாருங்கள்; நான் உங்களுக்கு இளைப்பாறுதல் தருவேன்.",
        devotionalEnglish = "Lay every burden at Jesus' feet this morning and receive His deep, restoring peace.",
        devotionalTamil = "இன்றைய கவலைகள் அனைத்தையும் இயேசுவின் பாதத்தில் வைத்து அவருடைய இளைப்பாறுதலைப் பெறுங்கள்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_11",
        bookReference = "Lamentations 3:22-23",
        textEnglish = "Through the Lord's mercies we are not consumed, because His compassions fail not. They are new every morning; great is Your faithfulness.",
        textTamil = "நாம் நிர்மூலமாகாதிருக்கிறது கர்த்தருடைய கிருபையே; அவருடைய இரக்கங்களுக்கு முடிவில்லை. அவைகள் காலைதோறும் புதியவைகள்; உமது உண்மை பெரியதாயிருக்கிறது.",
        devotionalEnglish = "Today brings fresh mercy—past mistakes are forgiven, and new grace waits for this morning.",
        devotionalTamil = "இன்று காலை கர்த்தருடைய புதிய கிருபையும் இரக்கமும் உங்கள் மீது இறங்குகிறது.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_12",
        bookReference = "Isaiah 40:31",
        textEnglish = "Those who wait on the Lord shall renew their strength; they shall mount up with wings like eagles.",
        textTamil = "கர்த்தருக்குக் காத்திருக்கிறவர்களோ புதுப்பெலன் அடைந்து, கழுகுகளைப்போலச் செட்டைகளை அடித்து எழும்புவார்கள்.",
        devotionalEnglish = "Quiet time in God's presence restores energy that no human effort can manufacture.",
        devotionalTamil = "கர்த்தருக்குக் காத்திருக்கும் ஜெப நேரம் நம் ஆத்துமாவுக்குப் புதிய பெலனைக் கொண்டுவருகிறது.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_13",
        bookReference = "John 14:27",
        textEnglish = "Peace I leave with you, My peace I give to you; not as the world gives do I give to you. Let not your heart be troubled.",
        textTamil = "சமாதானத்தை உங்களுக்கு வைத்துப்போகிறேன், என்னுடைய சமாதானத்தையே உங்களுக்குக் கொடுக்கிறேன்; உலகம் கொடுக்கிறபிரகாரம் நான் உங்களுக்குக் கொடுக்கிறதில்லை. உங்கள் இருதயம் கலங்காமலும் பயப்படாமலும் இருப்பதாக.",
        devotionalEnglish = "Christ's peace guards your mind even in the middle of busy responsibilities.",
        devotionalTamil = "பணிகளின் மத்தியிலும் கிறிஸ்துவின் சமாதானம் உங்கள் இருதயத்தைக் காத்துக்கொள்ளட்டும்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_14",
        bookReference = "Psalm 119:105",
        textEnglish = "Your word is a lamp to my feet and a light to my path.",
        textTamil = "உம்முடைய வசனம் என் கால்களுக்குத் தீபமும், என் பாதைக்கு வெளிச்சமுமாயிருக்கிறது.",
        devotionalEnglish = "Let scripture illuminate your next honest step today—even one verse brings clarity.",
        devotionalTamil = "வேத வசனம் இன்றைய நாளின் ஒவ்வொரு அடியிலும் நமக்கு வெளிச்சமாய் இருக்கிறது.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_15",
        bookReference = "1 Thessalonians 5:16-18",
        textEnglish = "Rejoice always, pray without ceasing, in everything give thanks; for this is the will of God in Christ Jesus for you.",
        textTamil = "எப்பொழுதும் சந்தோஷமாயிருங்கள். இடைவிடாமல் ஜெபம்பண்ணுங்கள். எல்லாவற்றிலேயும் ஸ்தோத்திரஞ்செய்யுங்கள்.",
        devotionalEnglish = "Gratitude shifts our eyes from what is lacking to how faithfully God provides.",
        devotionalTamil = "ஸ்தோத்திரமும் ஜெபமும் நம் வாழ்வில் கர்த்தருடைய ஆசீர்வாதத்தைத் திறக்கும் சாவியாகும்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_16",
        bookReference = "Philippians 4:6-7",
        textEnglish = "Be anxious for nothing, but in everything by prayer and supplication, with thanksgiving, let your requests be made known to God.",
        textTamil = "நீங்கள் ஒன்றுக்குங் கவலைப்படாமல், எல்லாவற்றையுங்குறித்து உங்கள் விண்ணப்பங்களை ஸ்தோத்திரத்தோடே கூடிய ஜெபத்தினாலும் வேண்டுதலினாலும் தேவனுக்குத் தெரியப்படுத்துங்கள்.",
        devotionalEnglish = "Trade every anxious thought for thankful prayer, and let God's peace stand guard over your home.",
        devotionalTamil = "கவலைகளை ஜெபமாக மாற்றும்போது தேவசமாதானம் உங்கள் குடும்பத்தைக் காக்கும்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_17",
        bookReference = "Psalm 27:1",
        textEnglish = "The Lord is my light and my salvation; whom shall I fear? The Lord is the strength of my life.",
        textTamil = "கர்த்தர் என் வெளிச்சமும் என் இரட்சிப்புமானவர், யாருக்குப் பயப்படுவேன்? கர்த்தர் என் ஜீவனின் பெலனானவர்.",
        devotionalEnglish = "Walk in quiet confidence today—when the Lord is your light, darkness cannot prevail.",
        devotionalTamil = "கர்த்தர் நம் வெளிச்சமாய் இருப்பதால் எந்த இருளுக்கும் நாம் அஞ்சத் தேவையில்லை.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_18",
        bookReference = "2 Corinthians 12:9",
        textEnglish = "My grace is sufficient for you, for My strength is made perfect in weakness.",
        textTamil = "என் கிருபை உனக்குப் போதும்; பலவீனத்திலே என் பலம் பூரணமாய் விளங்கும்.",
        devotionalEnglish = "Where your human strength stops, God's sufficient grace steps in with abundance.",
        devotionalTamil = "நம் பெலவீனம் முடிவடையும் இடத்தில் கர்த்தருடைய பூரண கிருபை வெளிப்படுகிறது.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_19",
        bookReference = "Psalm 37:4",
        textEnglish = "Delight yourself also in the Lord, and He shall give you the desires of your heart.",
        textTamil = "கர்த்தரிடத்தில் மனமகிழ்ச்சியாயிரு; அவர் உன் இருதயத்தின் வேண்டுதல்களை உனக்கு அருள்செய்வார்.",
        devotionalEnglish = "Root your joy in knowing the Lord first, and watch Him shape and fulfill your prayers.",
        devotionalTamil = "கர்த்தரில் மகிழ்ச்சியாய் இருக்கும்போது அவர் உங்கள் இருதயத்தின் ஆசைகளை நிறைவேற்றுவார்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_20",
        bookReference = "Colossians 3:23",
        textEnglish = "And whatever you do, do it heartily, as to the Lord and not to men.",
        textTamil = "எதைச் செய்தாலும், அதை மனுஷர்களுக்கென்று செய்யாமல், கர்த்தருக்கென்றே மனப்பூர்வமாய்ச் செய்யுங்கள்.",
        devotionalEnglish = "Every task today—whether at home, workplace, or church—becomes worship when done for Christ.",
        devotionalTamil = "நாம் செய்யும் ஒவ்வொரு சிறு பணியையும் கர்த்தருக்கென்று உண்மையுடன் செய்யும்போது அது ஆராதனையாகிறது.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_21",
        bookReference = "Psalm 91:1-2",
        textEnglish = "He who dwells in the secret place of the Most High shall abide under the shadow of the Almighty.",
        textTamil = "உன்னதமானவரின் மறைவிலிருக்கிறவன் சர்வவல்லவருடைய நிழலில் தங்குவான்.",
        devotionalEnglish = "Start each day anchored under God's protective shadow—a place of true safety and peace.",
        devotionalTamil = "சர்வவல்லவருடைய நிழலில் தங்கும் குடும்பத்திற்குப் பாதுகாப்பும் சமாதானமும் நிச்சயம் உண்டு.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_22",
        bookReference = "Matthew 6:33",
        textEnglish = "But seek first the kingdom of God and His righteousness, and all these things shall be added to you.",
        textTamil = "முதலாவது தேவனுடைய ராஜ்யத்தையும் அவருடைய நீதியையும் தேடுங்கள், அப்பொழுது இவைகளெல்லாம் உங்களுக்குக் கூடக் கொடுக்கப்படும்.",
        devotionalEnglish = "Put God first in your schedule and priorities today; He faithfully handles every daily need.",
        devotionalTamil = "நம் வாழ்வில் கர்த்தருக்கு முதலிடம் கொடுக்கும்போது மற்ற எல்லாத் தேவைகளையும் அவர் சந்திப்பார்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_23",
        bookReference = "Ephesians 4:32",
        textEnglish = "And be kind to one another, tenderhearted, forgiving one another, even as God in Christ forgave you.",
        textTamil = "ஒருவருக்கொருவர் தயவாயும் மனஉருக்கமாயும் இருந்து, கிறிஸ்துவுக்குள் தேவன் உங்களுக்கு மன்னித்ததுபோல, நீங்களும் ஒருவருக்கொருவர் மன்னியுங்கள்.",
        devotionalEnglish = "Share Christ's kindness and patience with your family and church community today.",
        devotionalTamil = "இயேசுவின் அன்பையும் மன்னிப்பையும் குடும்பத்திலும் சபையிலும் பகிர்ந்து கொள்வோம்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_24",
        bookReference = "Psalm 103:1-2",
        textEnglish = "Bless the Lord, O my soul, and all that is within me, bless His holy name! Bless the Lord, O my soul, and forget not all His benefits.",
        textTamil = "என் ஆத்துமாவே, கர்த்தரை ஸ்தோத்திரி; என் முழு உள்ளமே, அவருடைய பரிசுத்த நாமத்தை ஸ்தோத்திரி. அவர் செய்த சகல உபகாரங்களையும் மறவாதே.",
        devotionalEnglish = "Count three blessings God gave you this week and offer Him heartfelt praise right now.",
        devotionalTamil = "கர்த்தர் செய்த நன்மைகளை நினைத்துப் பார்த்து நன்றியுடன் அவரைத் துதியுங்கள்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_25",
        bookReference = "Hebrews 13:8",
        textEnglish = "Jesus Christ is the same yesterday, today, and forever.",
        textTamil = "இயேசு கிறிஸ்து நேற்றும் இன்றும் என்றும் மாறாதவராயிருக்கிறார்.",
        devotionalEnglish = "Seasons and circumstances change rapidly, yet Christ's faithfulness stays unmoved.",
        devotionalTamil = "உலகம் மாறினாலும் நம்மை நேசிக்கிற இயேசு கிறிஸ்து என்றும் மாறாதவராய் இருக்கிறார்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_26",
        bookReference = "Isaiah 26:3",
        textEnglish = "You will keep him in perfect peace, whose mind is stayed on You, because he trusts in You.",
        textTamil = "உம்மை உறுதியாய்ப் பற்றிக்கொண்ட மனதையுடையவன் உம்மையே நம்பியிருக்கிறபடியால், அவனைப் பூரண சமாதானத்துடன் காத்துக்கொள்வீர்.",
        devotionalEnglish = "Fix your thoughts on God's promises rather than fears, and experience steady inner calm.",
        devotionalTamil = "மனதை கர்த்தரின் வாக்குத்தத்தங்களில் ஊன்றவைக்க, பூரண சமாதானம் நம்மைக் காக்கும்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_27",
        bookReference = "2 Timothy 1:7",
        textEnglish = "For God has not given us a spirit of fear, but of power and of love and of a sound mind.",
        textTamil = "தேவன் நமக்குப் பயமுள்ள ஆவியைக் கொடாமல், பலமும் அன்பும் தெளிந்த புத்தியுமுள்ள ஆவியையே கொடுத்திருக்கிறார்.",
        devotionalEnglish = "Reject fear and embrace clarity, love, and spiritual strength through the Holy Spirit.",
        devotionalTamil = "பயத்தை நீக்கி, பரிசுத்த ஆவியானவர் தரும் பெலன், அன்பு, தெளிந்த புத்தியோடு முன்செல்லுங்கள்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_28",
        bookReference = "Psalm 34:8",
        textEnglish = "Oh, taste and see that the Lord is good; blessed is the man who trusts in Him!",
        textTamil = "கர்த்தர் நல்லவர் என்பதை ருசித்துப் பாருங்கள்; அவர்மேல் நம்பிக்கையாயிருக்கிற மனுஷன் பாக்கியவான்.",
        devotionalEnglish = "Step out in faith today and experience firsthand how good and faithful the Lord is.",
        devotionalTamil = "கர்த்தரை நம்பி அடியெடுத்து வையுங்கள்; அவருடைய நன்மையை உங்கள் வாழ்வில் காண்பீர்கள்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_29",
        bookReference = "Romans 15:13",
        textEnglish = "Now may the God of hope fill you with all joy and peace in believing, that you may abound in hope by the power of the Holy Spirit.",
        textTamil = "நம்பிக்கையின் தேவன் விசுவாசத்தினாலே உங்களை எல்லாவித சந்தோஷத்தினாலும் சமாதானத்தினாலும் நிரப்புவாராக.",
        devotionalEnglish = "Expect renewed hope today—the Holy Spirit overflows your heart with joy and peace.",
        devotionalTamil = "பரிசுத்த ஆவியின் வல்லமையால் உங்கள் உள்ளம் நம்பிக்கையாலும் சந்தோஷத்தாலும் நிரம்பட்டும்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_30",
        bookReference = "Psalm 118:24",
        textEnglish = "This is the day the Lord has made; we will rejoice and be glad in it.",
        textTamil = "கர்த்தர் உண்டுபண்ணின நாள் இதுவே; இதிலே களிகூர்ந்து மகிழக்கடவோம்.",
        devotionalEnglish = "Receive today as a gift crafted by God—greet each person with grace and gladness.",
        devotionalTamil = "இந்த நாளைக் கர்த்தர் தந்த ஈவாகப் பெற்று மகிழ்ச்சியோடும் நன்றியோடும் வாழ்வோம்.",
        publishedByPastor = "GRACE AG Auto Daily"
      ),
      BibleVerse(
        verseId = "auto_31",
        bookReference = "Numbers 6:24-26",
        textEnglish = "The Lord bless you and keep you; the Lord make His face shine upon you, and be gracious to you; the Lord lift up His countenance upon you, and give you peace.",
        textTamil = "கர்த்தர் உன்னை ஆசீர்வதித்து, உன்னைக் காக்கக்கடவர். கர்த்தர் தம்முடைய முகத்தை உன்மேல் பிரகாசிக்கப்பண்ணி, உன்மேல் கிருபையாயிருக்கக்கடவர். கர்த்தர் தம்முடைய முகத்தை உன்மேல் பிரசன்னமாக்கி, உனக்குச் சமாதானம் கட்டளையிடக்கடவர்.",
        devotionalEnglish = "May God's blessing, protection, and radiant peace rest over your entire household today.",
        devotionalTamil = "கர்த்தருடைய ஆசீர்வாதமும் பாதுகாப்பும் சமாதானமும் இன்றும் என்றும் உங்கள் குடும்பத்தில் தங்குவதாக.",
        publishedByPastor = "GRACE AG Auto Daily"
      )
    )
  }
}
