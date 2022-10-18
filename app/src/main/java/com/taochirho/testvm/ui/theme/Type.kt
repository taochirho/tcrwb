package com.taochirho.testvm.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Typography
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taochirho.testvm.R

// Set of Material typography styles to start with

val splineSans = FontFamily(
    Font(R.font.splinesans_variablefont_wght)
)

val brygada1918 = FontFamily(
    Font(R.font.brygada1918_variablefont_wght)
)

val Typography = Typography(
    h1 = TextStyle(
        fontFamily = brygada1918,
        fontWeight = FontWeight.SemiBold,
        fontSize = 96.sp,
        letterSpacing = -1.5.sp,
        color = Color.Blue
    ),

    h2 = TextStyle(
        fontFamily = splineSans,
        fontWeight = FontWeight.Bold,
        fontSize = 60.sp,
        letterSpacing = -0.5.sp
    ),

    h3 = TextStyle(
        fontFamily = splineSans,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = 0.sp
    ),

    h4 = TextStyle(
        fontFamily = splineSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        letterSpacing = 0.25.sp
    ),
    h5 = TextStyle(
        fontFamily = brygada1918,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    ),

    h6 = TextStyle(
        fontFamily = splineSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        ),

    body1 = TextStyle(
        fontFamily = splineSans,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        letterSpacing = 0.sp,
    ),


)