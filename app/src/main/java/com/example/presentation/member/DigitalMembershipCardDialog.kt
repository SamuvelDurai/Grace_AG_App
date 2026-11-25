package com.example.presentation.member

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.core.model.BelieverMember
import com.example.core.util.QrCodeGenerator
import com.example.core.util.launchActivitySafely
import com.example.presentation.common.MemberAvatar
import com.example.ui.theme.ClassicChurchBlue
import com.example.ui.theme.DeepRoyalNavy
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftBlueContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenContainer
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextDeepNavy
import com.example.ui.theme.TextIceBlue
import com.example.ui.theme.TextSlate
import com.example.ui.theme.TextWhite

@Composable
fun DigitalMembershipCardDialog(
  member: BelieverMember,
  onDismiss: () -> Unit,
  onShare: (() -> Unit)? = null,
  churchName: String = ""
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val qrMatrix = remember(member.verificationToken, member.membershipId) {
    QrCodeGenerator.generateMatrix(
      member.verificationToken.ifBlank {
        com.example.core.util.ChurchIdGenerator.verificationTokenFor(member.membershipId)
      },
      size = 25
    )
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp)
        .testTag("digital_membership_card_dialog"),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        // Card Header (Royal Navy Gradient)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.horizontalGradient(
                colors = listOf(DeepRoyalNavy, ClassicChurchBlue)
              )
            )
            .padding(18.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(Color.White),
                contentAlignment = Alignment.Center
              ) {
                Image(
                  painter = painterResource(id = R.drawable.ic_church_logo),
                  contentDescription = null,
                  modifier = Modifier.size(30.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = churchName.ifBlank { stringResource(id = R.string.church_name_default) },
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    letterSpacing = 0.5.sp
                  )
                )
                Text(
                  text = "DIGITAL MEMBERSHIP CARD",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = TextIceBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                  )
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = TextWhite
              )
            }
          }
        }

        // Card Content
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Member Avatar and Info
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            MemberAvatar(
              photoUrl = member.profilePhotoUrl,
              displayName = member.fullName,
              size = 72.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = member.fullName.ifBlank { "Church Member" },
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy,
                  fontSize = 20.sp
                )
              )

              Spacer(modifier = Modifier.height(4.dp))

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = SoftBlueContainer
              ) {
                Text(
                  text = member.membershipId.ifBlank { "Pending ID" },
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ClassicChurchBlue
                  ),
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = SuccessGreenContainer
            ) {
              Text(
                text = member.membershipStatus,
                style = MaterialTheme.typography.labelSmall.copy(
                  color = SuccessGreen,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))
          HorizontalDivider(color = SoftBlueBorder, thickness = 1.dp)
          Spacer(modifier = Modifier.height(14.dp))

          // Details Grid
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Baptism", style = MaterialTheme.typography.labelSmall.copy(color = TextSlate))
              Text(
                text = member.baptismStatus,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text("Member Since", style = MaterialTheme.typography.labelSmall.copy(color = TextSlate))
              Text(
                text = member.churchJoiningDate.ifBlank { "2023" },
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = TextDeepNavy
                )
              )
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // QR Code Display (Always high-contrast dark on white for reliable camera scanning)
          val qrModuleColor = DeepRoyalNavy
          Box(
            modifier = Modifier
              .size(164.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(Color.White)
              .border(BorderStroke(1.5.dp, SoftBlueBorder), RoundedCornerShape(14.dp))
              .padding(12.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.size(140.dp)) {
              val matrixSize = qrMatrix.size
              val cellSize = size.width / matrixSize

              for (r in 0 until matrixSize) {
                for (c in 0 until matrixSize) {
                  if (qrMatrix[r][c]) {
                    drawRect(
                      color = qrModuleColor,
                      topLeft = Offset(c * cellSize, r * cellSize),
                      size = Size(cellSize, cellSize)
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Scan to verify membership at church entrance",
            style = MaterialTheme.typography.labelSmall.copy(
              color = TextSlate,
              fontSize = 11.sp
            ),
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(18.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = onDismiss,
              modifier = Modifier.weight(1f).height(48.dp),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, SoftBlueBorder),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceWhite,
                contentColor = TextDeepNavy
              )
            ) {
              Text("Close", color = TextDeepNavy, fontWeight = FontWeight.SemiBold)
            }

            Button(
              onClick = {
                if (onShare != null) {
                  onShare()
                } else {
                  val shareText = "${churchName.ifBlank { "GRACE AG" }} Digital Membership Card\n" +
                    "Name: ${member.fullName.ifBlank { "Church Member" }}\n" +
                    "ID: ${member.membershipId.ifBlank { "Pending ID" }}\n" +
                    "Status: ${member.membershipStatus} • Baptism: ${member.baptismStatus}"
                  val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                  }
                  context.launchActivitySafely(android.content.Intent.createChooser(shareIntent, "Share Membership Card"), "No share app available")
                }
              },
              modifier = Modifier.weight(1f).height(48.dp).testTag("share_membership_card_button"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = ClassicChurchBlue,
                contentColor = TextWhite
              )
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Share", color = TextWhite, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

