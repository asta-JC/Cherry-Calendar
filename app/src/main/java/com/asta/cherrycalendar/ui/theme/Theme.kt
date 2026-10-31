package com.asta.cherrycalendar.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
@Composable fun CherryCalendarTheme(darkTheme:Boolean=isSystemInDarkTheme(),content:@Composable ()->Unit) {
val p=if(darkTheme) listOf(0xFFFF9DB5,0xFF3B1528,0xFF502A3B,0xFFFFD9E3,0xFF1C151D,0xFF261D27,0xFF392A36,0xFFF9E8EE,0xFFCAB6C0,0xFF9B828F,0xFF4B3845) else listOf(0xFFAF294A,0xFFFFFFFF,0xFFF9DEE5,0xFF652039,0xFFFCF8F5,0xFFFFFCFA,0xFFF1E7E7,0xFF422731,0xFF78616B,0xFF927780,0xFFE9DDDF)
val c=p.map{Color(it)}
val base=if(darkTheme)darkColorScheme() else lightColorScheme()
MaterialTheme(colorScheme=base.copy(primary=c[0],onPrimary=c[1],primaryContainer=c[2],onPrimaryContainer=c[3],secondary=c[0],onSecondary=c[1],secondaryContainer=c[6],onSecondaryContainer=c[7],tertiary=c[0],onTertiary=c[1],tertiaryContainer=c[2],onTertiaryContainer=c[3],background=c[4],onBackground=c[7],surface=c[5],onSurface=c[7],surfaceVariant=c[6],onSurfaceVariant=c[8],outline=c[9],outlineVariant=c[10],surfaceContainer=c[5],surfaceContainerLow=c[4],surfaceContainerHigh=c[6],surfaceContainerHighest=c[6],surfaceContainerLowest=c[4],inverseSurface=c[7],inverseOnSurface=c[4],inversePrimary=c[2],surfaceTint=c[0]),content=content)
}
