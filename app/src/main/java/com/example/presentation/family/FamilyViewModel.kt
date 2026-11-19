package com.example.presentation.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.BelieverMember
import com.example.core.model.Family
import com.example.core.model.FamilyMemberEntry
import com.example.core.model.MonthlySubscription
import com.example.core.model.SubscriptionStatus
import com.example.data.family.FamilyRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface FamilyUiState {
  data object Loading : FamilyUiState
  data class Success(
    val currentFamily: Family? = null,
    val allFamilies: List<Family> = emptyList()
  ) : FamilyUiState
  data class Error(val message: String) : FamilyUiState
}

class FamilyViewModel(
  private val familyRepository: FamilyRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow<FamilyUiState>(FamilyUiState.Success())
  val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

  private val _currentFamily = MutableStateFlow<Family?>(null)
  val currentFamily: StateFlow<Family?> = _currentFamily.asStateFlow()

  private val _families = MutableStateFlow<List<Family>>(emptyList())
  val families: StateFlow<List<Family>> = _families.asStateFlow()

  private val _isSaving = MutableStateFlow(false)
  val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  private var observeFamilyJob: Job? = null
  private var observeAllJob: Job? = null

  fun clearMessages() {
    _actionMessage.value = null
    _errorMessage.value = null
  }

  fun observeFamily(familyId: String) {
    observeFamilyJob?.cancel()
    _uiState.value = FamilyUiState.Loading
    observeFamilyJob = viewModelScope.launch {
      familyRepository.observeFamilyById(familyId)
        .catch { err ->
          val msg = err.localizedMessage ?: "Failed to load family details"
          _errorMessage.value = msg
          _uiState.value = FamilyUiState.Error(msg)
        }
        .collect { family ->
          _currentFamily.value = family
          _uiState.value = FamilyUiState.Success(
            currentFamily = family,
            allFamilies = _families.value
          )
        }
    }
  }

  fun observeMemberFamily(uid: String, fallbackFamilyId: String = "") {
    observeFamilyJob?.cancel()
    _uiState.value = FamilyUiState.Loading
    observeFamilyJob = viewModelScope.launch {
      familyRepository.observeFamilyForMember(uid, fallbackFamilyId)
        .catch { err ->
          val msg = err.localizedMessage ?: "Failed to load family details"
          _errorMessage.value = msg
          _uiState.value = FamilyUiState.Error(msg)
        }
        .collect { family ->
          _currentFamily.value = family
          _uiState.value = FamilyUiState.Success(
            currentFamily = family,
            allFamilies = _families.value
          )
        }
    }
  }

  fun observeChurchFamilies(churchId: String = "church_main") {
    observeAllJob?.cancel()
    _uiState.value = FamilyUiState.Loading
    observeAllJob = viewModelScope.launch {
      familyRepository.observeAllFamilies(churchId)
        .catch { err ->
          val msg = err.localizedMessage ?: "Failed to load church families"
          _errorMessage.value = msg
          _uiState.value = FamilyUiState.Error(msg)
        }
        .collect { list ->
          _families.value = list
          _uiState.value = FamilyUiState.Success(
            currentFamily = _currentFamily.value,
            allFamilies = list
          )
        }
    }
  }

  fun createOrUpdateFamily(
    family: Family,
    onResult: ((Result<Family>) -> Unit)? = null
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      val result = familyRepository.upsertFamily(family)
      _isSaving.value = false
      result.onSuccess { saved ->
        _currentFamily.value = saved
        _actionMessage.value = "Family record saved (${saved.resolvedFamilyName()})"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Failed to save family record"
      }
      onResult?.invoke(result)
    }
  }

  fun addDependentMember(
    familyId: String,
    name: String,
    relationship: String,
    dateOfBirth: String = "",
    gender: String = "",
    baptismStatus: String = "NOT_BAPTIZED",
    phoneNumber: String = "",
    onResult: ((Result<Family>) -> Unit)? = null
  ) {
    if (name.isBlank()) {
      _errorMessage.value = "Dependent member name is required."
      return
    }
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      val result = familyRepository.addDependentMember(
        familyId = familyId,
        member = FamilyMemberEntry(
          name = name.trim(),
          relationship = relationship.trim().ifBlank { "Family Member" },
          dateOfBirth = dateOfBirth.trim(),
          gender = gender.trim(),
          baptismStatus = baptismStatus.ifBlank { "NOT_BAPTIZED" },
          phoneNumber = phoneNumber.trim()
        )
      )
      _isSaving.value = false
      result.onSuccess { updated ->
        _currentFamily.value = updated
        _actionMessage.value = "${name.trim()} added to ${updated.resolvedFamilyName()}"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Failed to add dependent member"
      }
      onResult?.invoke(result)
    }
  }

  fun connectBelieverToFamily(
    familyId: String,
    targetMember: BelieverMember,
    relationshipRole: String,
    makeHead: Boolean = false,
    defaultHeadMember: BelieverMember? = null,
    onResult: ((Result<Family>) -> Unit)? = null
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      val result = familyRepository.connectBelieverToFamily(
        familyId = familyId,
        targetMember = targetMember,
        relationshipRole = relationshipRole,
        makeHead = makeHead,
        defaultHeadMember = defaultHeadMember
      )
      _isSaving.value = false
      result.onSuccess { updated ->
        _currentFamily.value = updated
        _actionMessage.value = "${targetMember.fullName} connected as $relationshipRole"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Failed to connect believer to family"
      }
      onResult?.invoke(result)
    }
  }

  fun updateMemberRole(
    familyId: String,
    entryIdOrUid: String,
    newRole: String,
    promoteToHead: Boolean = false,
    onResult: ((Result<Family>) -> Unit)? = null
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      val result = familyRepository.updateMemberRoleInFamily(
        familyId = familyId,
        entryIdOrUid = entryIdOrUid,
        newRole = newRole,
        promoteToHead = promoteToHead
      )
      _isSaving.value = false
      result.onSuccess { updated ->
        _currentFamily.value = updated
        _actionMessage.value = "Family role updated to $newRole"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Failed to update family role"
      }
      onResult?.invoke(result)
    }
  }

  fun removeDependentMember(
    familyId: String,
    dependentId: String,
    onResult: ((Result<Family>) -> Unit)? = null
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      val result = familyRepository.removeDependentMember(familyId, dependentId)
      _isSaving.value = false
      result.onSuccess { updated ->
        _currentFamily.value = updated
        _actionMessage.value = "Family member removed"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Failed to remove dependent member"
      }
      onResult?.invoke(result)
    }
  }

  fun assignHouseholdHead(
    familyId: String,
    headUid: String,
    headMemberId: String,
    headName: String,
    headPhone: String = "",
    onResult: ((Result<Family>) -> Unit)? = null
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      val result = familyRepository.assignHouseholdHead(
        familyId = familyId,
        headUid = headUid,
        headMemberId = headMemberId,
        headName = headName,
        headPhone = headPhone
      )
      _isSaving.value = false
      result.onSuccess { updated ->
        _currentFamily.value = updated
        _actionMessage.value = "${updated.headName} assigned as household head"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Failed to update household head"
      }
      onResult?.invoke(result)
    }
  }

  fun linkMemberAccount(
    familyId: String,
    memberUid: String,
    onResult: ((Result<Family>) -> Unit)? = null
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      val result = familyRepository.linkAccountUidToFamily(familyId, memberUid)
      _isSaving.value = false
      result.onSuccess { updated ->
        _currentFamily.value = updated
        _actionMessage.value = "Member account linked to ${updated.resolvedFamilyName()}"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Failed to link member to family"
      }
      onResult?.invoke(result)
    }
  }

  fun updateSharedSubscriptionStatus(
    familyId: String,
    status: SubscriptionStatus,
    subscriptionId: String = "",
    monthYear: String = "",
    paidDate: String = "",
    paidByUid: String = "",
    paidByName: String = "",
    monthlyDuesAmount: Double? = null,
    onResult: ((Result<Family>) -> Unit)? = null
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      val result = familyRepository.updateSharedSubscriptionStatus(
        familyId = familyId,
        status = status,
        subscriptionId = subscriptionId,
        monthYear = monthYear,
        paidDate = paidDate,
        paidByUid = paidByUid,
        paidByName = paidByName,
        monthlyDuesAmount = monthlyDuesAmount
      )
      _isSaving.value = false
      result.onSuccess { updated ->
        _currentFamily.value = updated
        _actionMessage.value = "Family subscription status updated to ${status.name}"
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Failed to update shared subscription status"
      }
      onResult?.invoke(result)
    }
  }

  fun syncFromBelieverMember(
    member: BelieverMember,
    subscription: MonthlySubscription? = null,
    onResult: ((Result<Family>) -> Unit)? = null
  ) {
    viewModelScope.launch {
      _isSaving.value = true
      _errorMessage.value = null
      val result = familyRepository.syncFromBelieverMember(member, subscription)
      _isSaving.value = false
      result.onSuccess { synced ->
        _currentFamily.value = synced
      }.onFailure { err ->
        _errorMessage.value = err.localizedMessage ?: "Failed to sync family profile"
      }
      onResult?.invoke(result)
    }
  }
}
