package com.example.core.model

enum class PastoralCareType {
  BIRTHDAY,
  WEDDING_ANNIVERSARY,
  HOSPITAL_VISIT,
  HOME_BLESSING,
  BEREAVEMENT_CARE
}

enum class PastoralCareStatus {
  PENDING,
  CALLED,
  VISITED,
  PRAYED
}

data class PastoralCareAlert(
  val alertId: String = "",
  val churchId: String = "church_main",
  val memberUid: String = "",
  val memberName: String = "",
  val memberPhone: String = "",
  val membershipId: String = "",
  val careType: PastoralCareType = PastoralCareType.BIRTHDAY,
  val eventDate: String = "Today", // e.g. "12 Nov" or "Today"
  val milestoneYear: Int? = null, // e.g. 25th wedding anniversary or 60th birthday
  val notes: String = "",
  val locationDetails: String = "", // Hospital room, home address
  val status: PastoralCareStatus = PastoralCareStatus.PENDING,
  val isToday: Boolean = true
) {
  fun generateBlessingMessage(isTamil: Boolean): String {
    return when (careType) {
      PastoralCareType.BIRTHDAY -> {
        if (isTamil) {
          "அன்பான ${memberName}, இனிய பிறந்தநாள் நல்வாழ்த்துகள்! 'கர்த்தர் உன்னை ஆசீர்வதித்து, உன்னைக் காக்கக்கடவர்' - எண்ணாகமம் 6:24. உங்கள் வாழ்வில் கர்த்தருடைய நன்மையும் கிருபையும் பெருக ஜெபிக்கிறோம்! - சபை போதகர் & குடும்பத்தார்"
        } else {
          "Dear ${memberName}, Blessed Happy Birthday! 'The LORD bless you and keep you; The LORD make His face shine upon you' - Numbers 6:24. Praying for God's abundant peace and joy in your life! - Pastor & Church Family"
        }
      }
      PastoralCareType.WEDDING_ANNIVERSARY -> {
        val yearText = if (milestoneYear != null) " (${milestoneYear}th Anniversary)" else ""
        if (isTamil) {
          "அன்பான ${memberName} & குடும்பத்தினரே, இனிய திருமண நாள் நல்வாழ்த்துகள்! 'அவர்கள் இனி இருவராயிராமல், ஒரே மாம்சமாயிருக்கிறார்கள்; ஆகையால், தேவன் இணைத்ததை மனுஷன் பிரிக்காதிருக்கக்கடவன்' - மத்தேயு 19:6. உங்கள் குடும்பத்தை கர்த்தர் மென்மேலும் ஆசீர்வதிப்பாராக! - சபை போதகர்"
        } else {
          "Dear ${memberName} & family, Blessed Wedding Anniversary${yearText}! 'What God has joined together, let not man separate' - Matthew 19:6. May your home remain an altar of love and peace! - Pastor & Church Family"
        }
      }
      PastoralCareType.HOSPITAL_VISIT -> {
        if (isTamil) {
          "அன்பான ${memberName}, உங்கள் சுகத்திற்காகவும் பூரண நற்சுகத்திற்காகவும் சபையாக தொடர்ந்து ஜெபிக்கிறோம். 'நானே உன் பரிகாரியாகிய கர்த்தர்' - யாத்திராகமம் 15:26. விரைவில் தேவனுடைய பரிபூரண சுகம் உண்டாகட்டும்! - சபை போதகர்"
        } else {
          "Dear ${memberName}, our church family is holding you in prayer for rapid recovery and complete healing. 'For I am the LORD who heals you' - Exodus 15:26. Peace and strength be upon you! - Pastor"
        }
      }
      PastoralCareType.HOME_BLESSING -> {
        if (isTamil) {
          "அன்பான ${memberName}, உங்கள் இல்லத்தில் கர்த்தருடைய சமாதானமும் பிரசன்னமும் என்றும் வாசமாயிருக்க ஜெபிக்கிறோம். 'நானும் என் வீட்டாருமோவென்றால், கர்த்தரையே சேவிப்போம்' - யோசுவா 24:15. - சபை போதகர்"
        } else {
          "Dear ${memberName}, praying for God's everlasting peace and protection over your household! 'As for me and my house, we will serve the LORD' - Joshua 24:15. - Pastor"
        }
      }
      PastoralCareType.BEREAVEMENT_CARE -> {
        if (isTamil) {
          "அன்பான ${memberName}, உங்கள் குடும்பத்திற்கு பரிசுத்த ஆவியானவரின் ஆறுதலும் தேறுதலும் உண்டாகட்டும். 'துயரப்படுகிறவர்கள் பாக்கியவான்கள்; அவர்கள் ஆறுதலடைவார்கள்' - மத்தேயு 5:4. சபை எப்போதும் உங்களோடு நிற்கிறது. - சபை போதகர்"
        } else {
          "Dear ${memberName}, praying for the Holy Spirit's divine comfort and peace for your family during this time. 'Blessed are those who mourn, for they shall be comforted' - Matthew 5:4. - Pastor & Church Family"
        }
      }
    }
  }
}
