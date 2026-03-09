import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object YamiboIcons {
    val Home =
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(10.0f, 20.0f)
                verticalLineTo(14.0f)
                horizontalLineTo(14.0f)
                verticalLineTo(20.0f)
                horizontalLineTo(19.0f)
                verticalLineTo(12.0f)
                horizontalLineTo(22.0f)
                lineTo(12.0f, 3.0f)
                lineTo(2.0f, 12.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(20.0f)
                horizontalLineTo(10.0f)
                close()
            }
        }.build()

    val Message =
        ImageVector.Builder(
            name = "Message",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(20.0f, 2.0f)
                horizontalLineTo(4.0f)
                curveTo(2.9f, 2.0f, 2.0f, 2.9f, 2.0f, 4.0f)
                verticalLineTo(22.0f)
                lineTo(6.0f, 18.0f)
                horizontalLineTo(20.0f)
                curveTo(21.1f, 18.0f, 22.0f, 17.1f, 22.0f, 16.0f)
                verticalLineTo(4.0f)
                curveTo(22.0f, 2.9f, 21.1f, 2.0f, 20.0f, 2.0f)
                close()
                moveTo(20.0f, 16.0f)
                horizontalLineTo(6.0f)
                lineTo(4.0f, 18.0f)
                verticalLineTo(4.0f)
                horizontalLineTo(20.0f)
                verticalLineTo(16.0f)
                close()
            }
        }.build()

    val Profile =
        ImageVector.Builder(
            name = "Profile",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12.0f, 12.0f)
                curveTo(14.21f, 12.0f, 16.0f, 10.21f, 16.0f, 8.0f)
                curveTo(16.0f, 5.79f, 14.21f, 4.0f, 12.0f, 4.0f)
                curveTo(9.79f, 4.0f, 8.0f, 5.79f, 8.0f, 8.0f)
                curveTo(8.0f, 10.21f, 9.79f, 12.0f, 12.0f, 12.0f)
                close()
                moveTo(12.0f, 14.0f)
                curveTo(9.33f, 14.0f, 4.0f, 15.34f, 4.0f, 18.0f)
                verticalLineTo(20.0f)
                horizontalLineTo(20.0f)
                verticalLineTo(18.0f)
                curveTo(20.0f, 15.34f, 14.67f, 14.0f, 12.0f, 14.0f)
                close()
            }
        }.build()

    val Search: ImageVector =
        ImageVector.Builder(
            name = "Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1f
            ) {
                moveTo(11.742f, 10.344f)
                arcTo(6.5f, 6.5f, 0f, true, false, 10.345f, 11.742f)
                horizontalLineToRelative(-0.001f)
                curveToRelative(0.03f, 0.04f, 0.062f, 0.078f, 0.098f, 0.115f)
                lineToRelative(3.85f, 3.85f)
                arcToRelative(1f, 1f, 0f, false, false, 1.415f, -1.414f)
                lineToRelative(-3.85f, -3.85f)
                arcToRelative(1.007f, 1.007f, 0f, false, false, -0.115f, -0.1f)
                close()
                moveTo(12f, 6.5f)
                arcToRelative(5.5f, 5.5f, 0f, true, true, -11f, 0f)
                arcToRelative(5.5f, 5.5f, 0f, false, true, 11f, 0f)
                close()
            }
        }.build()

    val Views: ImageVector =
        ImageVector.Builder(
            name = "Views",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Outer eye shape
                moveTo(991f, 515f)
                quadToRelative(-11f, 19f, -25f, 39f)
                quadToRelative(-20f, 28f, -42f, 55f)
                quadToRelative(-27f, 33f, -57f, 63f)
                quadToRelative(-35f, 35f, -72f, 63f)
                quadToRelative(-42f, 32f, -85f, 55f)
                quadToRelative(-49f, 26f, -99f, 39f)
                quadToRelative(-55f, 15f, -111f, 15f)
                reflectiveQuadToRelative(-111f, -15f)
                quadToRelative(-50f, -13f, -99f, -39f)
                quadToRelative(-43f, -23f, -85f, -55f)
                quadToRelative(-37f, -28f, -72f, -63f)
                quadToRelative(-30f, -30f, -57f, -63f)
                quadToRelative(-22f, -27f, -42f, -55f)
                quadToRelative(-14f, -20f, -25f, -39f)
                lineToRelative(-9f, -15f)
                lineToRelative(9f, -15f)
                quadToRelative(11f, -19f, 25f, -39f)
                quadToRelative(20f, -28f, 42f, -55f)
                quadToRelative(27f, -33f, 57f, -63f)
                quadToRelative(35f, -35f, 71f, -63f)
                quadToRelative(42f, -32f, 86f, -55f)
                quadToRelative(49f, -26f, 99f, -39f)
                quadToRelative(55f, -15f, 111f, -15f)
                reflectiveQuadToRelative(111f, 15f)
                quadToRelative(50f, 13f, 99f, 39f)
                quadToRelative(44f, 23f, 86f, 55f)
                quadToRelative(36f, 28f, 71f, 63f)
                quadToRelative(30f, 30f, 57f, 63f)
                quadToRelative(22f, 27f, 42f, 55f)
                quadToRelative(14f, 20f, 25f, 39f)
                quadToRelative(5f, 8f, 9f, 15f)
                close()
                // Inner detail - top arc
                moveTo(181f, 631f)
                verticalLineToRelative(0f)
                quadToRelative(70f, 69f, 144f, 108f)
                quadToRelative(90f, 48f, 181f, 48f)
                quadToRelative(95f, 0f, 184f, -48f)
                quadToRelative(70f, -37f, 141f, -108f)
                quadToRelative(66f, -66f, 94f, -112f)
                quadToRelative(3f, -3f, 6f, -9.5f)
                reflectiveQuadToRelative(7f, -9.5f)
                quadToRelative(-52f, -71f, -107f, -125f)
                quadToRelative(-70f, -70f, -141f, -108f)
                quadToRelative(-90f, -48f, -184f, -48f)
                reflectiveQuadToRelative(-183f, 48f)
                quadToRelative(-70f, 37f, -142f, 108f)
                quadToRelative(-29f, 25f, -57f, 60f)
                quadToRelative(-20f, 23f, -49f, 65f)
                quadToRelative(57f, 81f, 106f, 131f)
                close()
                // Inner pupil ring
                moveTo(579f, 365f)
                quadToRelative(36f, 21f, 56.5f, 56.5f)
                reflectiveQuadTo(656f, 500f)
                reflectiveQuadToRelative(-21f, 78.5f)
                reflectiveQuadToRelative(-56.5f, 56.5f)
                reflectiveQuadToRelative(-78.5f, 21f)
                reflectiveQuadToRelative(-78.5f, -20.5f)
                reflectiveQuadTo(365f, 579f)
                reflectiveQuadToRelative(-21f, -79f)
                reflectiveQuadToRelative(21f, -79f)
                reflectiveQuadToRelative(56.5f, -56.5f)
                reflectiveQuadTo(500f, 344f)
                reflectiveQuadToRelative(79f, 21f)
                close()
                // Outer iris ring
                moveTo(311f, 608.5f)
                quadToRelative(30f, 50.5f, 80.5f, 80.5f)
                reflectiveQuadToRelative(109f, 30f)
                reflectiveQuadToRelative(109f, -29f)
                reflectiveQuadToRelative(79.5f, -78f)
                quadToRelative(30f, -51f, 30f, -112f)
                reflectiveQuadToRelative(-30f, -112f)
                quadToRelative(-29f, -49f, -79.5f, -78f)
                reflectiveQuadTo(500f, 281f)
                reflectiveQuadTo(390.5f, 310f)
                reflectiveQuadTo(311f, 388f)
                quadToRelative(-30f, 51f, -30f, 112f)
                quadToRelative(0f, 58f, 30f, 108.5f)
                close()
            }
        }.build()

    val Comment: ImageVector =
        ImageVector.Builder(
            name = "Comment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 5.5f)
                curveTo(2f, 4.1f, 3.1f, 3f, 4.5f, 3f)
                horizontalLineToRelative(15f)
                curveTo(20.9f, 3f, 22f, 4.1f, 22f, 5.5f)
                verticalLineToRelative(10f)
                curveToRelative(0f, 1.4f, -1.1f, 2.5f, -2.5f, 2.5f)
                horizontalLineToRelative(-3.1f)
                curveToRelative(-0.4f, 0f, -0.8f, 0.2f, -1f, 0.5f)
                lineTo(13f, 21.6f)
                curveToRelative(-0.4f, 0.6f, -1.2f, 0.7f, -1.8f, 0.3f)
                lineToRelative(-0.3f, -0.3f)
                lineToRelative(-2.4f, -3.2f)
                curveToRelative(-0.1f, -0.2f, -0.5f, -0.4f, -0.9f, -0.4f)
                horizontalLineTo(4.5f)
                curveTo(3.1f, 18f, 2f, 16.9f, 2f, 15.5f)
                verticalLineToRelative(-10f)
                close()
                moveTo(8.2f, 10.5f)
                curveToRelative(0f, -0.7f, -0.6f, -1.3f, -1.3f, -1.3f)
                reflectiveCurveToRelative(-1.3f, 0.6f, -1.3f, 1.3f)
                reflectiveCurveToRelative(0.6f, 1.3f, 1.3f, 1.3f)
                reflectiveCurveToRelative(1.3f, -0.6f, 1.3f, -1.3f)
                close()
                moveTo(13.2f, 10.5f)
                curveToRelative(0f, -0.7f, -0.6f, -1.3f, -1.3f, -1.3f)
                reflectiveCurveToRelative(-1.3f, 0.6f, -1.3f, 1.3f)
                reflectiveCurveToRelative(0.6f, 1.3f, 1.3f, 1.3f)
                reflectiveCurveToRelative(1.3f, -0.6f, 1.3f, -1.3f)
                close()
                moveTo(17f, 11.7f)
                curveToRelative(0.7f, 0f, 1.3f, -0.6f, 1.3f, -1.3f)
                reflectiveCurveToRelative(-0.6f, -1.2f, -1.3f, -1.2f)
                reflectiveCurveToRelative(-1.3f, 0.6f, -1.3f, 1.3f)
                reflectiveCurveToRelative(0.6f, 1.2f, 1.3f, 1.2f)
                close()
            }
        }.build()

    val PersonFill: ImageVector =
        ImageVector.Builder(
            name = "PersonFill",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(813f, 875f)
                lineToRelative(10f, -1f)
                quadToRelative(11f, -2f, 21f, -7f)
                quadToRelative(14f, -7f, 21f, -19f)
                quadToRelative(10f, -15f, 10f, -36f)
                quadToRelative(0f, -29f, -15f, -65f)
                quadToRelative(-19f, -46f, -55f, -83f)
                quadToRelative(-46f, -46f, -115f, -72f)
                quadToRelative(-81f, -30f, -190f, -30f)
                reflectiveQuadToRelative(-190f, 30f)
                quadToRelative(-69f, 26f, -114f, 71f)
                quadToRelative(-37f, 37f, -56f, 83f)
                quadToRelative(-15f, 36f, -15f, 66f)
                quadToRelative(0f, 22f, 10f, 36f)
                quadToRelative(8f, 12f, 22f, 19f)
                quadToRelative(9f, 5f, 21f, 7f)
                quadToRelative(5f, 1f, 10f, 1f)
                close()
                moveTo(405f, 475f)
                quadToRelative(-43f, -25f, -67.5f, -68f)
                reflectiveQuadTo(313f, 312f)
                reflectiveQuadToRelative(24.5f, -95f)
                reflectiveQuadToRelative(67.5f, -67.5f)
                reflectiveQuadToRelative(95f, -24.5f)
                reflectiveQuadToRelative(95f, 24.5f)
                reflectiveQuadToRelative(68f, 67f)
                reflectiveQuadToRelative(25f, 95.5f)
                reflectiveQuadToRelative(-25f, 96f)
                reflectiveQuadToRelative(-68f, 67.5f)
                reflectiveQuadToRelative(-95f, 24.5f)
                reflectiveQuadToRelative(-95f, -25f)
                close()
            }
        }.build()

    val StarOutline: ImageVector =
        ImageVector.Builder(
            name = "StarOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(231f, 631f)
                lineTo(13f, 419f)
                quadToRelative(-13f, -9f, -14f, -21f)
                reflectiveQuadToRelative(8f, -22.5f)
                reflectiveQuadToRelative(24f, -13.5f)
                lineToRelative(307f, -43f)
                lineToRelative(131f, -269f)
                lineToRelative(19f, -19f)
                quadToRelative(9f, -5f, 21.5f, 0f)
                reflectiveQuadTo(531f, 50f)
                lineToRelative(132f, 275f)
                lineToRelative(306f, 44f)
                quadToRelative(15f, 0f, 22.5f, 10f)
                reflectiveQuadToRelative(6.5f, 23.5f)
                reflectiveQuadToRelative(-10f, 22.5f)
                lineTo(769f, 637f)
                lineToRelative(50f, 294f)
                quadToRelative(3f, 15f, -3f, 26f)
                reflectiveQuadToRelative(-17.5f, 14.5f)
                reflectiveQuadTo(775f, 969f)
                lineTo(500f, 825f)
                lineTo(225f, 963f)
                quadToRelative(-9f, 6f, -21f, 1.5f)
                reflectiveQuadToRelative(-19f, -16f)
                reflectiveQuadToRelative(-4f, -23.5f)
                close()
                moveTo(500.5f, 752.5f)
                quadTo(508f, 754f, 513f, 762f)
                lineToRelative(231f, 119f)
                lineToRelative(-44f, -250f)
                quadToRelative(0f, -9f, 4f, -17.5f)
                reflectiveQuadToRelative(9f, -13.5f)
                lineToRelative(181f, -175f)
                lineToRelative(-256f, -38f)
                quadToRelative(-9f, 0f, -14.5f, -4f)
                reflectiveQuadTo(613f, 369f)
                lineTo(500f, 137f)
                lineTo(388f, 362f)
                quadToRelative(-6f, 10f, -11f, 14.5f)
                reflectiveQuadToRelative(-14f, 4.5f)
                lineToRelative(-257f, 38f)
                lineToRelative(182f, 175f)
                quadToRelative(8f, 4f, 11f, 13f)
                quadToRelative(1f, 5f, 1f, 18f)
                lineToRelative(-44f, 250f)
                lineToRelative(232f, -119f)
                quadToRelative(5f, -5f, 12.5f, -3.5f)
                close()
            }
        }.build()

    val StarFilled: ImageVector =
        ImageVector.Builder(
            name = "StarFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1000f,
            viewportHeight = 1000f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(500f, 825f)
                lineTo(781f, 963f)
                quadToRelative(9f, 6f, 21f, 1.5f)
                reflectiveQuadToRelative(19f, -16f)
                reflectiveQuadToRelative(4f, -23.5f)
                lineToRelative(-50f, -294f)
                lineToRelative(219f, -212f)
                quadToRelative(9f, -9f, 9f, -21f)
                reflectiveQuadToRelative(-8f, -22.5f)
                reflectiveQuadToRelative(-20f, -13.5f)
                lineToRelative(-306f, -43f)
                lineTo(531f, 50f)
                quadToRelative(-6f, -12f, -17f, -16.5f)
                reflectiveQuadToRelative(-22f, 0f)
                reflectiveQuadTo(475f, 50f)
                lineTo(338f, 319f)
                lineTo(31f, 362f)
                quadToRelative(-15f, 3f, -24f, 13.5f)
                reflectiveQuadTo(-1f, 398f)
                reflectiveQuadToRelative(14f, 21f)
                lineToRelative(218f, 212f)
                lineToRelative(-50f, 294f)
                quadToRelative(-3f, 15f, 3f, 26f)
                reflectiveQuadToRelative(17.5f, 14.5f)
                reflectiveQuadToRelative(23.5f, -2.5f)
                close()
            }
        }.build()

    val Share: ImageVector =
        ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 25f,
            viewportHeight = 25f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Arrow diagonal line
                moveTo(18.9697f, 4.96967f)
                lineTo(11.9697f, 11.9697f)
                lineTo(13.0303f, 13.0303f)
                lineTo(20.0303f, 6.03033f)
                close()
                // Top-right arrow vertical
                moveTo(18.75f, 5.5f)
                verticalLineTo(9.641f)
                horizontalLineTo(20.25f)
                verticalLineTo(5.5f)
                close()
                // Top-right arrow horizontal
                moveTo(19.5f, 4.75f)
                horizontalLineTo(15.412f)
                verticalLineTo(6.25f)
                horizontalLineTo(19.5f)
                close()
                // Rounded rect path
                moveTo(12.5f, 4.75f)
                horizontalLineTo(9.5f)
                curveTo(6.87665f, 4.75f, 4.75f, 6.87665f, 4.75f, 9.5f)
                verticalLineTo(15.5f)
                curveTo(4.75f, 18.1234f, 6.87665f, 20.25f, 9.5f, 20.25f)
                horizontalLineTo(15.5f)
                curveTo(18.1234f, 20.25f, 20.25f, 18.1234f, 20.25f, 15.5f)
                verticalLineTo(12.5f)
                horizontalLineTo(18.75f)
                verticalLineTo(15.5f)
                curveTo(18.75f, 17.2949f, 17.2949f, 18.75f, 15.5f, 18.75f)
                horizontalLineTo(9.5f)
                curveTo(7.70507f, 18.75f, 6.25f, 17.2949f, 6.25f, 15.5f)
                verticalLineTo(9.5f)
                curveTo(6.25f, 7.70507f, 7.70507f, 6.25f, 9.5f, 6.25f)
                horizontalLineTo(12.5f)
                close()
            }
        }.build()

    val Explore =
        ImageVector.Builder(
            name = "Explore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(14.19f, 14.19f)
                lineTo(6f, 18f)
                lineToRelative(3.81f, -8.19f)
                lineTo(18f, 6f)
                lineToRelative(-3.81f, 8.19f)
                close()
                moveTo(12f, 10.9f)
                curveToRelative(-0.61f, 0f, -1.1f, 0.49f, -1.1f, 1.1f)
                reflectiveCurveToRelative(0.49f, 1.1f, 1.1f, 1.1f)
                curveToRelative(0.61f, 0f, 1.1f, -0.49f, 1.1f, -1.1f)
                reflectiveCurveToRelative(-0.49f, -1.1f, -1.1f, -1.1f)
                close()
            }
        }.build()

    val ThreeDots =
        ImageVector.Builder(
            name = "ThreeDots",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9.5f, 13f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -3f, dy1 = 0f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 0f)
                close()
                moveTo(9.5f, 8f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -3f, dy1 = 0f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 0f)
                close()
                moveTo(9.5f, 3f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -3f, dy1 = 0f)
                arcToRelative(1.5f, 1.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 3f, dy1 = 0f)
                close()
            }
        }.build()

    val Heart =
        ImageVector.Builder(
            name = "Heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 1024f,
            viewportHeight = 1024f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(923f, 283f)
                curveToRelative(0f, -132f, -107f, -239f, -239f, -239f)
                curveToRelative(-80f, 0f, -151f, 39f, -195f, 99f)
                curveToRelative(-44f, -60f, -115f, -99f, -195f, -99f)
                curveToRelative(-132f, 0f, -239f, 107f, -239f, 239f)
                curveToRelative(0f, 247f, 434f, 493f, 434f, 493f)
                reflectiveCurveToRelative(434f, -246f, 434f, -493f)
                close()
            }
        }.build()
}
