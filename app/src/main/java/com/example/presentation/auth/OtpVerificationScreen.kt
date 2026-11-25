package com.example.presentation.auth

import android.widget.Toast
import com.example.core.util.findActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite
import com.example.ui.theme.VibrantBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
  viewModel: AuthViewModel,
  onVerificationSuccess: () -> Unit,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context.findActivity()
  val phone by viewModel.phoneNumber.collectAsState()
  val timer by viewModel.timerSeconds.collectAsState()
  val isLoading by viewModel.isPhoneLoading.collectAsState()
  val phoneError by viewModel.phoneError.collectAsState()

  var otpInput by remember { mutableStateOf("") }
  var localError by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(id = R.string.phone_login_title),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("otp_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = ClassicChurchBlue
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = CleanBackground)
      )
    },
    containerColor = CleanBackground,
    modifier = modifier.testTag("otp_verification_screen")
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 24.dp)
        .imePadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Top
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = stringResource(id = R.string.otp_title),
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          color = TextDeepNavy,
          fontSize = 24.sp
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = stringResource(id = R.string.otp_subtitle, phone),
        style = MaterialTheme.typography.bodyMedium.copy(
          color = TextSlate,
          lineHeight = 22.sp
        ),
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(24.dp))

      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(
          modifier = Modifier.padding(22.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          OutlinedTextField(
            value = otpInput,
            onValueChange = { raw ->
              otpInput = raw.filter { c -> c.isDigit() }.take(6)
              localError = null
              if (phoneError != null) viewModel.clearAuthMessages()
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("otp_input_field"),
            label = { Text("6-Digit OTP", color = TextSlate) },
            placeholder = { Text("••••••", color = com.example.ui.theme.TextMuted) },
            textStyle = TextStyle(
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 6.sp,
              textAlign = TextAlign.Center,
              color = TextDeepNavy
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextDeepNavy,
              unfocusedTextColor = TextDeepNavy,
              focusedBorderColor = ClassicChurchBlue,
              unfocusedBorderColor = SoftBlueBorder,
              focusedLabelColor = ClassicChurchBlue,
              unfocusedLabelColor = TextSlate,
              focusedContainerColor = SurfaceWhite,
              unfocusedContainerColor = SurfaceWhite,
              cursorColor = ClassicChurchBlue
            ),
            isError = localError != null || phoneError != null,
            supportingText = {
              val displayErr = localError ?: phoneError
              if (!displayErr.isNullOrBlank()) {
                Text(
                  text = displayErr,
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
              }
            }
          )

          Spacer(modifier = Modifier.height(22.dp))

          Button(
            onClick = {
              viewModel.verifyPhoneOtp(
                code = otpInput,
                onSuccess = onVerificationSuccess,
                onError = { err -> localError = err }
              )
            },
            enabled = otpInput.length == 6 && !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("verify_otp_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = ClassicChurchBlue,
              contentColor = TextWhite,
              disabledContainerColor = ClassicChurchBlue.copy(alpha = 0.38f),
              disabledContentColor = TextWhite.copy(alpha = 0.72f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = TextWhite,
                strokeWidth = 2.dp
              )
            } else {
              Text(
                text = stringResource(id = R.string.verify_otp),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextWhite
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          if (timer > 0) {
            Text(
              text = stringResource(id = R.string.resend_timer, timer),
              style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSlate,
                fontWeight = FontWeight.Medium
              ),
              modifier = Modifier.padding(vertical = 8.dp)
            )
          } else {
            TextButton(
              onClick = {
                val act = activity
                if (act == null) {
                  Toast.makeText(context, "Unable to resend OTP right now.", Toast.LENGTH_SHORT).show()
                } else {
                  viewModel.sendPhoneOtp(phone = phone, activity = act)
                }
              },
              modifier = Modifier.testTag("resend_otp_button")
            ) {
              Text(
                text = stringResource(id = R.string.resend_otp),
                style = MaterialTheme.typography.titleSmall.copy(
                  color = VibrantBlue,
                  fontWeight = FontWeight.Bold
                )
              )
            }
          }
        }
      }
    }
  }
}
