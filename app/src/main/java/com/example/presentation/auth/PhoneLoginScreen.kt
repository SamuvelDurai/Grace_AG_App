package com.example.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.util.PhoneNumberHelper
import com.example.core.util.findActivity
import com.example.presentation.common.AnimatedEntranceItem
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.CleanBackground
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainer
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextIceBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun PhoneLoginScreen(
  viewModel: AuthViewModel,
  onNavigateToOtp: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = context.findActivity()

  // 0 = Sign In (Pastors, Super Admin & Existing Believers), 1 = Believer Sign Up
  var authMode by remember { mutableIntStateOf(0) }
  var emailInput by remember { mutableStateOf("") }
  var passwordInput by remember { mutableStateOf("") }
  var confirmPasswordInput by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var confirmPasswordVisible by remember { mutableStateOf(false) }
  var phoneInput by remember { mutableStateOf("") }
  var selectedCountry by remember { mutableStateOf(PhoneNumberHelper.SUPPORTED_COUNTRIES.first()) }
  var countryMenuExpanded by remember { mutableStateOf(false) }

  val isPhoneLoading by viewModel.isPhoneLoading.collectAsState()
  val isEmailLoading by viewModel.isEmailLoading.collectAsState()
  val phoneError by viewModel.phoneError.collectAsState()
  val authActionError by viewModel.authActionError.collectAsState()
  val authInfoMessage by viewModel.authInfoMessage.collectAsState()
  val authState by viewModel.uiState.collectAsState()
  val isGoogleLoading = authState is AuthUiState.Loading
  val googleError = (authState as? AuthUiState.Unauthenticated)?.message
  val scrollState = rememberScrollState()

  val fieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextDeepNavy,
    unfocusedTextColor = TextDeepNavy,
    focusedBorderColor = ClassicChurchBlue,
    unfocusedBorderColor = SoftBlueBorder,
    focusedLabelColor = ClassicChurchBlue,
    unfocusedLabelColor = TextSlate,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    cursorColor = ClassicChurchBlue
  )

  val isSignUpMode = authMode == 1

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag("phone_login_screen"),
    color = CleanBackground
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp, vertical = 20.dp)
        .imePadding(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Top
    ) {
      // Sanctuary Hero Header Card
      AnimatedEntranceItem(index = 0) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = DeepRoyalNavy),
          elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(176.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_sanctuary_hero),
              contentDescription = stringResource(id = R.string.app_name),
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colorStops = arrayOf(
                      0.0f to DeepRoyalNavy.copy(alpha = 0.30f),
                      0.50f to DeepRoyalNavy.copy(alpha = 0.72f),
                      1.0f to DeepRoyalNavy.copy(alpha = 0.94f)
                    )
                  )
                )
            )
            Column(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(66.dp)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                  contentAlignment = Alignment.Center
                ) {
                  Image(
                    painter = painterResource(id = R.drawable.ic_church_logo),
                    contentDescription = null,
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = if (isSignUpMode) {
                  stringResource(id = R.string.believer_signup_title)
                } else {
                  stringResource(id = R.string.auth_signin_title)
                },
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextWhite,
                  fontSize = 21.sp
                ),
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(4.dp))

              Text(
                text = if (isSignUpMode) {
                  stringResource(id = R.string.believer_signup_subtitle)
                } else {
                  stringResource(id = R.string.auth_signin_subtitle)
                },
                style = MaterialTheme.typography.bodySmall.copy(
                  color = TextIceBlue,
                  lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Mode Toggle: Sign In vs Believer Sign Up
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = SoftBlueContainer,
        border = BorderStroke(1.dp, SoftBlueBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Button(
            onClick = {
              authMode = 0
              viewModel.clearAuthMessages()
            },
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("auth_mode_sign_in"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (!isSignUpMode) ClassicChurchBlue else Color.Transparent,
              contentColor = if (!isSignUpMode) TextWhite else TextDeepNavy
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
          ) {
            Text(
              text = stringResource(id = R.string.sign_in),
              color = if (!isSignUpMode) TextWhite else TextDeepNavy,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
          }

          Button(
            onClick = {
              authMode = 1
              viewModel.clearAuthMessages()
            },
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("auth_mode_believer_signup"),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSignUpMode) ClassicChurchBlue else Color.Transparent,
              contentColor = if (isSignUpMode) TextWhite else TextDeepNavy
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
          ) {
            Text(
              text = stringResource(id = R.string.believer_signup_tab),
              color = if (isSignUpMode) TextWhite else TextDeepNavy,
              fontWeight = FontWeight.Bold,
              maxLines = 1
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      val activeError = authActionError ?: phoneError ?: googleError
      if (!activeError.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = ErrorRedContainer,
          border = BorderStroke(1.dp, ErrorRed),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = activeError,
            color = ErrorRed,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(12.dp)
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      if (!authInfoMessage.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = SuccessGreenContainer,
          border = BorderStroke(1.dp, SuccessGreen),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = authInfoMessage!!,
            color = SuccessGreen,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(12.dp)
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      // Card 1: Email & Password Sign-In / Believer Sign-Up
      AnimatedEntranceItem(index = 1) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
              )
            ),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
          border = BorderStroke(1.dp, SoftBlueBorder)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = if (isSignUpMode) {
              "New Believer Account (Email & Password)"
            } else {
              "Email & Password (Pastors, Admin & Members)"
            },
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = emailInput,
            onValueChange = {
              emailInput = it
              if (authActionError != null) viewModel.clearAuthMessages()
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("email_input_field"),
            label = { Text("Email Address", color = TextSlate) },
            placeholder = { Text("name@church.org", color = TextMuted) },
            leadingIcon = {
              Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = ClassicChurchBlue)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            colors = fieldColors
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = passwordInput,
            onValueChange = {
              passwordInput = it
              if (authActionError != null) viewModel.clearAuthMessages()
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("password_input_field"),
            label = { Text("Password", color = TextSlate) },
            leadingIcon = {
              Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = ClassicChurchBlue)
            },
            trailingIcon = {
              IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.size(48.dp)
              ) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                  tint = TextSlate
                )
              }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            colors = fieldColors
          )

          if (isSignUpMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
              value = confirmPasswordInput,
              onValueChange = {
                confirmPasswordInput = it
                if (authActionError != null) viewModel.clearAuthMessages()
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_password_input_field"),
              label = { Text("Confirm Password", color = TextSlate) },
              leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = ClassicChurchBlue)
              },
              trailingIcon = {
                IconButton(
                  onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                  modifier = Modifier.size(48.dp)
                ) {
                  Icon(
                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (confirmPasswordVisible) "Hide Password" else "Show Password",
                    tint = TextSlate
                  )
                }
              },
              visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              colors = fieldColors
            )
          } else {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
              TextButton(
                onClick = { viewModel.sendPasswordReset(emailInput) },
                enabled = !isEmailLoading,
                modifier = Modifier.testTag("forgot_password_button")
              ) {
                Text(
                  text = "Forgot Password?",
                  color = ClassicChurchBlue,
                  fontWeight = FontWeight.Bold,
                  style = MaterialTheme.typography.labelMedium
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(if (isSignUpMode) 16.dp else 6.dp))

          Button(
            onClick = {
              if (isSignUpMode) {
                viewModel.signUpWithEmail(emailInput, passwordInput, confirmPasswordInput)
              } else {
                viewModel.signInWithEmail(emailInput, passwordInput)
              }
            },
            enabled = !isEmailLoading && !isGoogleLoading && emailInput.isNotBlank() && passwordInput.isNotBlank(),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("email_auth_submit_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = ClassicChurchBlue,
              contentColor = TextWhite,
              disabledContainerColor = ClassicChurchBlue.copy(alpha = 0.38f),
              disabledContentColor = TextWhite.copy(alpha = 0.72f)
            )
          ) {
            if (isEmailLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = TextWhite,
                strokeWidth = 2.dp
              )
            } else {
              Text(
                text = if (isSignUpMode) {
                  stringResource(id = R.string.create_believer_account)
                } else {
                  stringResource(id = R.string.sign_in)
                },
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextWhite
                )
              )
            }
          }
        }
      }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = SoftBlueBorder)
        Text(
          text = stringResource(id = R.string.or_divider),
          modifier = Modifier.padding(horizontal = 16.dp),
          style = MaterialTheme.typography.labelMedium.copy(
            color = TextSlate,
            fontWeight = FontWeight.Bold
          )
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = SoftBlueBorder)
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Card 2: Mobile OTP Verification
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SoftBlueBorder)
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = if (isSignUpMode) "Verify Mobile Number to Sign Up" else "Mobile Number OTP Login",
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = TextDeepNavy
            )
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = phoneInput,
            onValueChange = { raw ->
              phoneInput = PhoneNumberHelper.sanitizeNationalInput(raw, selectedCountry.dialCode)
              if (phoneError != null) viewModel.clearAuthMessages()
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("phone_input_field"),
            label = {
              Text(
                text = stringResource(id = R.string.phone_label),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
              )
            },
            leadingIcon = {
              Box {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clickable { countryMenuExpanded = true }
                    .padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = ClassicChurchBlue,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = selectedCountry.dialCode,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = TextDeepNavy
                    )
                  )
                  Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select country code",
                    tint = ClassicChurchBlue,
                    modifier = Modifier.size(18.dp)
                  )
                }
                DropdownMenu(
                  expanded = countryMenuExpanded,
                  onDismissRequest = { countryMenuExpanded = false },
                  modifier = Modifier.background(SurfaceWhite)
                ) {
                  PhoneNumberHelper.SUPPORTED_COUNTRIES.forEach { option ->
                    DropdownMenuItem(
                      text = {
                        Text(
                          text = option.label,
                          color = TextDeepNavy,
                          fontWeight = if (option == selectedCountry) FontWeight.Bold else FontWeight.Medium
                        )
                      },
                      onClick = {
                        selectedCountry = option
                        phoneInput = PhoneNumberHelper.sanitizeNationalInput(phoneInput, option.dialCode)
                        countryMenuExpanded = false
                      }
                    )
                  }
                }
              }
            },
            placeholder = { Text(stringResource(id = R.string.phone_placeholder), color = TextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            colors = fieldColors,
            isError = phoneError != null,
            supportingText = {
              if (phoneError != null) {
                Text(
                  text = phoneError ?: "",
                  color = ErrorRed,
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
              } else {
                Text(
                  text = "Enter ${selectedCountry.minNationalDigits}-digit mobile number (OTP SMS)",
                  color = TextSlate,
                  style = MaterialTheme.typography.labelSmall
                )
              }
            }
          )

          Spacer(modifier = Modifier.height(12.dp))

          val isPhoneReady = PhoneNumberHelper.toE164OrNull(phoneInput, selectedCountry.dialCode) != null
          Button(
            onClick = {
              val act = activity
              if (act == null) {
                Toast.makeText(context, "Unable to verify mobile number right now. Please try again.", Toast.LENGTH_SHORT).show()
                return@Button
              }
              viewModel.sendPhoneOtp(
                phone = phoneInput,
                activity = act,
                countryCode = selectedCountry.dialCode,
                onCodeSent = onNavigateToOtp
              )
            },
            enabled = isPhoneReady && !isPhoneLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("send_otp_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = ClassicChurchBlue,
              contentColor = TextWhite,
              disabledContainerColor = ClassicChurchBlue.copy(alpha = 0.38f),
              disabledContentColor = TextWhite.copy(alpha = 0.72f)
            )
          ) {
            if (isPhoneLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = TextWhite,
                strokeWidth = 2.dp
              )
            } else {
              Text(
                text = stringResource(id = R.string.send_otp),
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextWhite
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Google Sign-In / Sign-Up interactive button
      OutlinedButton(
        onClick = {
          val act = activity
          if (act == null) {
            Toast.makeText(context, "Unable to launch Google Sign-In.", Toast.LENGTH_SHORT).show()
            return@OutlinedButton
          }
          viewModel.signInWithGoogle(act)
        },
        enabled = !isGoogleLoading && !isEmailLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("google_sign_in_button"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, ClassicChurchBlue),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = SurfaceWhite,
          contentColor = ClassicChurchBlue
        )
      ) {
        if (isGoogleLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = ClassicChurchBlue,
            strokeWidth = 2.dp
          )
        } else {
          Text(
            text = if (isSignUpMode) "Sign up with Google" else stringResource(id = R.string.sign_in_google),
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.Bold,
              color = ClassicChurchBlue
            )
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = stringResource(id = R.string.auth_cloud_sync_note),
        style = MaterialTheme.typography.labelSmall.copy(
          color = TextSlate,
          lineHeight = 16.sp
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
