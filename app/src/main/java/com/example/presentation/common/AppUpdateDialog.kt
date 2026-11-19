package com.example.presentation.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.BuildConfig
import com.example.R
import com.example.core.model.ChurchConfig
import com.example.core.util.launchActivitySafely
import com.example.data.admin.ChurchAdminRepository
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun AppUpdateChecker(
  churchAdminRepository: ChurchAdminRepository,
  churchId: String,
  currentVersionCode: Int = BuildConfig.VERSION_CODE,
  currentVersionName: String = BuildConfig.VERSION_NAME
) {
  val churchConfig by churchAdminRepository.observeChurchConfig(churchId)
    .collectAsState(initial = ChurchConfig(churchId = churchId))
  var dismissedVersionCode by remember { mutableIntStateOf(-1) }

  val shouldShow = churchConfig.hasUpdateAvailable(currentVersionCode) &&
    (churchConfig.forceUpdate || dismissedVersionCode != churchConfig.latestVersionCode)

  if (shouldShow) {
    AppUpdateDialog(
      churchConfig = churchConfig,
      currentVersionName = currentVersionName,
      onDismiss = {
        if (!churchConfig.forceUpdate) {
          dismissedVersionCode = churchConfig.latestVersionCode
        }
      }
    )
  }
}

@Composable
fun AppUpdateDialog(
  churchConfig: ChurchConfig,
  currentVersionName: String = BuildConfig.VERSION_NAME,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = {
      if (!churchConfig.forceUpdate) {
        onDismiss()
      }
    },
    properties = DialogProperties(
      dismissOnBackPress = !churchConfig.forceUpdate,
      dismissOnClickOutside = !churchConfig.forceUpdate
    ),
    containerColor = SurfaceWhite,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = SoftBlueContainer
        ) {
          Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = null,
            tint = ClassicChurchBlue,
            modifier = Modifier
              .padding(8.dp)
              .size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = stringResource(id = R.string.update_available_title),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
          Text(
            text = "v$currentVersionName -> v${churchConfig.latestVersionName}",
            style = MaterialTheme.typography.labelSmall.copy(
              color = ClassicChurchBlue,
              fontWeight = FontWeight.Bold
            )
          )
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SoftBlueContainer,
          border = BorderStroke(1.dp, SoftBlueBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = if (churchConfig.forceUpdate) {
                "Mandatory Church Update (v${churchConfig.latestVersionName})"
              } else {
                "What's New in v${churchConfig.latestVersionName}"
              },
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ClassicChurchBlue
              )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = churchConfig.updateReleaseNotes.ifBlank {
                "A newer version of ${churchConfig.name.ifBlank { "GRACE AG" }} is ready. Tap Download Update to install the latest APK directly."
              },
              style = MaterialTheme.typography.bodySmall.copy(
                color = TextDeepNavy,
                lineHeight = 19.sp
              )
            )
          }
        }
        Text(
          text = "Opens the direct church APK link in your browser—no Play Store required.",
          style = MaterialTheme.typography.labelSmall.copy(color = TextSlate)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val url = churchConfig.updateDownloadUrl.trim()
          val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
          context.launchActivitySafely(intent, "No browser found to open update link")
          if (!churchConfig.forceUpdate) {
            onDismiss()
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("app_update_download_button")
      ) {
        Text(
          text = stringResource(id = R.string.update_download_now),
          color = TextWhite,
          fontWeight = FontWeight.Bold
        )
      }
    },
    dismissButton = if (!churchConfig.forceUpdate) {
      {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, SoftBlueBorder),
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("app_update_later_button")
        ) {
          Text(
            text = stringResource(id = R.string.update_later),
            color = TextDeepNavy,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    } else {
      null
    }
  )
}
