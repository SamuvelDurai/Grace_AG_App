package com.example.presentation.status

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.model.AccountStatus
import com.example.core.model.UserProfile
import com.example.core.util.launchActivitySafely
import com.example.presentation.auth.AuthViewModel
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun PendingApprovalScreen(
  userProfile: UserProfile,
  viewModel: AuthViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("pending_approval_screen"),
    color = CleanBackground
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(90.dp)
          .clip(CircleShape)
          .background(SoftBlueContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.HourglassTop,
          contentDescription = null,
          tint = ClassicChurchBlue,
          modifier = Modifier.size(46.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = stringResource(id = R.string.pending_approval_title),
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy,
          fontSize = 24.sp
        ),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = stringResource(id = R.string.pending_approval_msg),
        style = MaterialTheme.typography.bodyMedium.copy(
          color = TextSlate,
          lineHeight = 22.sp
        ),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = userProfile.displayName.ifBlank { "Applicant" },
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )

          Spacer(modifier = Modifier.height(6.dp))

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = SoftBlueContainer
          ) {
            Text(
              text = stringResource(
                id = R.string.membership_id_label,
                userProfile.membershipId.ifBlank { "Pending Assignment" }
              ),
              style = MaterialTheme.typography.bodySmall.copy(
                color = ClassicChurchBlue,
                fontWeight = FontWeight.Bold
              ),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      Button(
        onClick = {
          viewModel.refreshProfile { status ->
            val msg = when (status) {
              AccountStatus.APPROVED -> "Membership approved! Loading your dashboard..."
              AccountStatus.PENDING -> "Status checked — still awaiting Pastor/Admin review."
              else -> "Status updated."
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("check_status_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = ClassicChurchBlue,
          contentColor = TextWhite
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
      ) {
        Text(
          text = stringResource(id = R.string.check_status),
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = TextWhite
          )
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedButton(
        onClick = {
          val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:+914424567890")
          }
          context.launchActivitySafely(dialIntent, "Church helpline: +91 44 2456 7890")
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("contact_church_button"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ClassicChurchBlue),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceWhite,
          contentColor = ClassicChurchBlue
        )
      ) {
        Text(
          text = stringResource(id = R.string.contact_church),
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            color = ClassicChurchBlue
          )
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedButton(
        onClick = { viewModel.signOut(context) },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("sign_out_button"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceWhite,
          contentColor = ErrorRed
        )
      ) {
        Text(
          text = stringResource(id = R.string.sign_out),
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            color = ErrorRed
          )
        )
      }
    }
  }
}
