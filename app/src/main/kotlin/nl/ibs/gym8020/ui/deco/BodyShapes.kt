package nl.ibs.gym8020.ui.deco

import nl.ibs.gym8020.core.model.Muscle

/*
 * Body figure for the muscle heatmap (D-40, D-43): own vector drawing, a stylized art deco mosaic.
 * Grid 100 × 212 per view. Shapes without a muscle are the neutral parts (head, hands, knees, …).
 * Generated from the design sketch; edit both together.
 */

class BodyShape(val muscle: Muscle?, val points: List<Pair<Float, Float>>)

val bodyFront: List<BodyShape> = listOf(
    BodyShape(null, listOf(58.5f to 14.0f, 58.0f to 17.6f, 56.5f to 20.7f, 54.2f to 23.1f, 51.5f to 24.3f, 48.5f to 24.3f, 45.8f to 23.1f, 43.5f to 20.7f, 42.0f to 17.6f, 41.5f to 14.0f, 42.0f to 10.4f, 43.5f to 7.3f, 45.8f to 4.9f, 48.5f to 3.7f, 51.5f to 3.7f, 54.2f to 4.9f, 56.5f to 7.3f, 58.0f to 10.4f)),
    BodyShape(null, listOf(44f to 26f, 50f to 25f, 50f to 32f, 44f to 32f)),
    BodyShape(null, listOf(56f to 32f, 50f to 32f, 50f to 25f, 56f to 26f)),
    BodyShape(null, listOf(38f to 104f, 50f to 106f, 50f to 114f, 36f to 112f)),
    BodyShape(null, listOf(64f to 112f, 50f to 114f, 50f to 106f, 62f to 104f)),
    BodyShape(null, listOf(23.5f to 101.0f, 23.3f to 102.7f, 22.7f to 104.2f, 21.8f to 105.3f, 20.6f to 105.9f, 19.4f to 105.9f, 18.2f to 105.3f, 17.3f to 104.2f, 16.7f to 102.7f, 16.5f to 101.0f, 16.7f to 99.3f, 17.3f to 97.8f, 18.2f to 96.7f, 19.4f to 96.1f, 20.6f to 96.1f, 21.7f to 96.7f, 22.7f to 97.8f, 23.3f to 99.3f)),
    BodyShape(null, listOf(76.7f to 99.3f, 77.3f to 97.8f, 78.3f to 96.7f, 79.4f to 96.1f, 80.6f to 96.1f, 81.8f to 96.7f, 82.7f to 97.8f, 83.3f to 99.3f, 83.5f to 101.0f, 83.3f to 102.7f, 82.7f to 104.2f, 81.8f to 105.3f, 80.6f to 105.9f, 79.4f to 105.9f, 78.2f to 105.3f, 77.3f to 104.2f, 76.7f to 102.7f, 76.5f to 101.0f)),
    BodyShape(null, listOf(44.5f to 165.0f, 44.2f to 166.4f, 43.4f to 167.6f, 42.2f to 168.5f, 40.8f to 168.9f, 39.2f to 168.9f, 37.8f to 168.5f, 36.6f to 167.6f, 35.8f to 166.4f, 35.5f to 165.0f, 35.8f to 163.6f, 36.6f to 162.4f, 37.8f to 161.5f, 39.2f to 161.1f, 40.8f to 161.1f, 42.2f to 161.5f, 43.4f to 162.4f, 44.2f to 163.6f)),
    BodyShape(null, listOf(55.8f to 163.6f, 56.6f to 162.4f, 57.8f to 161.5f, 59.2f to 161.1f, 60.8f to 161.1f, 62.2f to 161.5f, 63.4f to 162.4f, 64.2f to 163.6f, 64.5f to 165.0f, 64.2f to 166.4f, 63.4f to 167.6f, 62.2f to 168.5f, 60.8f to 168.9f, 59.2f to 168.9f, 57.8f to 168.5f, 56.6f to 167.6f, 55.8f to 166.4f, 55.5f to 165.0f)),
    BodyShape(null, listOf(36f to 170f, 44f to 170f, 42f to 200f, 37f to 200f)),
    BodyShape(null, listOf(63f to 200f, 58f to 200f, 56f to 170f, 64f to 170f)),
    BodyShape(null, listOf(35f to 201f, 43f to 201f, 44f to 207f, 31f to 207f)),
    BodyShape(null, listOf(69f to 207f, 56f to 207f, 57f to 201f, 65f to 201f)),
    BodyShape(Muscle.FRONT_DELT, listOf(37f to 34f, 44f to 33f, 42f to 42f, 35f to 47f, 31f to 44f)),
    BodyShape(Muscle.FRONT_DELT, listOf(69f to 44f, 65f to 47f, 58f to 42f, 56f to 33f, 63f to 34f)),
    BodyShape(Muscle.SIDE_DELT, listOf(31f to 36f, 37f to 34f, 31f to 44f, 27f to 52f, 25f to 45f)),
    BodyShape(Muscle.SIDE_DELT, listOf(75f to 45f, 73f to 52f, 69f to 44f, 63f to 34f, 69f to 36f)),
    BodyShape(Muscle.CHEST, listOf(49f to 36f, 44f to 34f, 38f to 38f, 35f to 48f, 40f to 56f, 49f to 57f)),
    BodyShape(Muscle.CHEST, listOf(51f to 57f, 60f to 56f, 65f to 48f, 62f to 38f, 56f to 34f, 51f to 36f)),
    BodyShape(Muscle.BICEPS, listOf(32.6f to 62.6f, 31.8f to 66.0f, 30.6f to 68.9f, 29.1f to 70.9f, 27.4f to 71.9f, 25.8f to 71.6f, 24.5f to 70.3f, 23.6f to 67.9f, 23.2f to 64.8f, 23.4f to 61.4f, 24.2f to 58.0f, 25.4f to 55.1f, 26.9f to 53.1f, 28.6f to 52.1f, 30.2f to 52.4f, 31.5f to 53.7f, 32.4f to 56.1f, 32.8f to 59.2f)),
    BodyShape(Muscle.BICEPS, listOf(67.2f to 59.2f, 67.6f to 56.1f, 68.5f to 53.7f, 69.8f to 52.4f, 71.4f to 52.1f, 73.1f to 53.1f, 74.6f to 55.1f, 75.8f to 58.0f, 76.6f to 61.4f, 76.8f to 64.8f, 76.4f to 67.9f, 75.5f to 70.3f, 74.2f to 71.6f, 72.6f to 71.9f, 70.9f to 70.9f, 69.4f to 68.9f, 68.2f to 66.0f, 67.4f to 62.6f)),
    BodyShape(Muscle.FOREARMS, listOf(24f to 73f, 31f to 74f, 27f to 97f, 21f to 96f)),
    BodyShape(Muscle.FOREARMS, listOf(79f to 96f, 73f to 97f, 69f to 74f, 76f to 73f)),
    BodyShape(Muscle.ABS, listOf(43.5f to 59f, 49f to 59f, 49f to 99f, 44f to 97f)),
    BodyShape(Muscle.ABS, listOf(56f to 97f, 51f to 99f, 51f to 59f, 56.5f to 59f)),
    BodyShape(Muscle.OBLIQUES, listOf(42.5f to 60f, 38f to 60f, 35f to 72f, 37f to 92f, 43f to 97f)),
    BodyShape(Muscle.OBLIQUES, listOf(57f to 97f, 63f to 92f, 65f to 72f, 62f to 60f, 57.5f to 60f)),
    BodyShape(Muscle.QUADS, listOf(37f to 112f, 48f to 115f, 46f to 140f, 44f to 160f, 37f to 162f, 33f to 140f)),
    BodyShape(Muscle.QUADS, listOf(67f to 140f, 63f to 162f, 56f to 160f, 54f to 140f, 52f to 115f, 63f to 112f)),
    BodyShape(Muscle.ADDUCTORS, listOf(49f to 115f, 48f to 116f, 47f to 136f, 49f to 130f)),
    BodyShape(Muscle.ADDUCTORS, listOf(51f to 130f, 53f to 136f, 52f to 116f, 51f to 115f)),
)

val bodyBack: List<BodyShape> = listOf(
    BodyShape(null, listOf(58.5f to 14.0f, 58.0f to 17.6f, 56.5f to 20.7f, 54.2f to 23.1f, 51.5f to 24.3f, 48.5f to 24.3f, 45.8f to 23.1f, 43.5f to 20.7f, 42.0f to 17.6f, 41.5f to 14.0f, 42.0f to 10.4f, 43.5f to 7.3f, 45.8f to 4.9f, 48.5f to 3.7f, 51.5f to 3.7f, 54.2f to 4.9f, 56.5f to 7.3f, 58.0f to 10.4f)),
    BodyShape(null, listOf(44f to 26f, 50f to 25f, 50f to 30f, 44f to 30f)),
    BodyShape(null, listOf(56f to 30f, 50f to 30f, 50f to 25f, 56f to 26f)),
    BodyShape(null, listOf(23.5f to 101.0f, 23.3f to 102.7f, 22.7f to 104.2f, 21.8f to 105.3f, 20.6f to 105.9f, 19.4f to 105.9f, 18.2f to 105.3f, 17.3f to 104.2f, 16.7f to 102.7f, 16.5f to 101.0f, 16.7f to 99.3f, 17.3f to 97.8f, 18.2f to 96.7f, 19.4f to 96.1f, 20.6f to 96.1f, 21.7f to 96.7f, 22.7f to 97.8f, 23.3f to 99.3f)),
    BodyShape(null, listOf(76.7f to 99.3f, 77.3f to 97.8f, 78.3f to 96.7f, 79.4f to 96.1f, 80.6f to 96.1f, 81.8f to 96.7f, 82.7f to 97.8f, 83.3f to 99.3f, 83.5f to 101.0f, 83.3f to 102.7f, 82.7f to 104.2f, 81.8f to 105.3f, 80.6f to 105.9f, 79.4f to 105.9f, 78.2f to 105.3f, 77.3f to 104.2f, 76.7f to 102.7f, 76.5f to 101.0f)),
    BodyShape(null, listOf(44.0f to 166.0f, 43.8f to 167.0f, 43.1f to 167.9f, 42.0f to 168.6f, 40.7f to 169.0f, 39.3f to 169.0f, 38.0f to 168.6f, 36.9f to 167.9f, 36.2f to 167.0f, 36.0f to 166.0f, 36.2f to 165.0f, 36.9f to 164.1f, 38.0f to 163.4f, 39.3f to 163.0f, 40.7f to 163.0f, 42.0f to 163.4f, 43.1f to 164.1f, 43.8f to 165.0f)),
    BodyShape(null, listOf(56.2f to 165.0f, 56.9f to 164.1f, 58.0f to 163.4f, 59.3f to 163.0f, 60.7f to 163.0f, 62.0f to 163.4f, 63.1f to 164.1f, 63.8f to 165.0f, 64.0f to 166.0f, 63.8f to 167.0f, 63.1f to 167.9f, 62.0f to 168.6f, 60.7f to 169.0f, 59.3f to 169.0f, 58.0f to 168.6f, 56.9f to 167.9f, 56.2f to 167.0f, 56.0f to 166.0f)),
    BodyShape(null, listOf(35f to 201f, 43f to 201f, 44f to 207f, 31f to 207f)),
    BodyShape(null, listOf(69f to 207f, 56f to 207f, 57f to 201f, 65f to 201f)),
    BodyShape(Muscle.LATS, listOf(38f to 46f, 41f to 58f, 45f to 63f, 47f to 79f, 42f to 90f, 37f to 82f, 35f to 60f, 35f to 50f)),
    BodyShape(Muscle.LATS, listOf(65f to 50f, 65f to 60f, 63f to 82f, 58f to 90f, 53f to 79f, 55f to 63f, 59f to 58f, 62f to 46f)),
    BodyShape(Muscle.TRAPS, listOf(50f to 24f, 50f to 58f, 44f to 50f, 38f to 38f, 34f to 35f, 44f to 30f)),
    BodyShape(Muscle.TRAPS, listOf(56f to 30f, 66f to 35f, 62f to 38f, 56f to 50f, 50f to 58f, 50f to 24f)),
    BodyShape(Muscle.REAR_DELT, listOf(34f to 35f, 38f to 38f, 35f to 48f, 29f to 50f, 28f to 42f)),
    BodyShape(Muscle.REAR_DELT, listOf(72f to 42f, 71f to 50f, 65f to 48f, 62f to 38f, 66f to 35f)),
    BodyShape(Muscle.SIDE_DELT, listOf(28f to 40f, 29f to 50f, 26f to 54f, 24f to 46f)),
    BodyShape(Muscle.SIDE_DELT, listOf(76f to 46f, 74f to 54f, 71f to 50f, 72f to 40f)),
    BodyShape(Muscle.MID_BACK, listOf(44f to 40f, 49.5f to 56f, 49.5f to 62f, 42f to 58f, 40f to 44f)),
    BodyShape(Muscle.MID_BACK, listOf(60f to 44f, 58f to 58f, 50.5f to 62f, 50.5f to 56f, 56f to 40f)),
    BodyShape(Muscle.TRICEPS, listOf(32.8f to 62.5f, 32.1f to 66.0f, 31.0f to 69.1f, 29.4f to 71.3f, 27.7f to 72.4f, 26.1f to 72.2f, 24.7f to 70.8f, 23.6f to 68.3f, 23.1f to 65.1f, 23.2f to 61.5f, 23.9f to 58.0f, 25.0f to 54.9f, 26.6f to 52.7f, 28.3f to 51.6f, 29.9f to 51.8f, 31.3f to 53.2f, 32.4f to 55.7f, 32.9f to 58.9f)),
    BodyShape(Muscle.TRICEPS, listOf(67.1f to 58.9f, 67.6f to 55.7f, 68.7f to 53.2f, 70.1f to 51.8f, 71.7f to 51.6f, 73.4f to 52.7f, 75.0f to 54.9f, 76.1f to 58.0f, 76.8f to 61.5f, 76.9f to 65.1f, 76.4f to 68.3f, 75.3f to 70.8f, 73.9f to 72.2f, 72.3f to 72.4f, 70.6f to 71.3f, 69.0f to 69.1f, 67.9f to 66.0f, 67.2f to 62.5f)),
    BodyShape(Muscle.FOREARMS, listOf(24f to 74f, 31f to 74f, 27f to 97f, 21f to 96f)),
    BodyShape(Muscle.FOREARMS, listOf(79f to 96f, 73f to 97f, 69f to 74f, 76f to 74f)),
    BodyShape(Muscle.LOWER_BACK, listOf(49.5f to 66f, 49.5f to 98f, 44f to 98f, 43f to 90f, 48f to 80f)),
    BodyShape(Muscle.LOWER_BACK, listOf(52f to 80f, 57f to 90f, 56f to 98f, 50.5f to 98f, 50.5f to 66f)),
    BodyShape(Muscle.GLUTES, listOf(50.5f to 112.0f, 50.0f to 115.1f, 48.7f to 117.8f, 46.8f to 119.8f, 44.3f to 120.9f, 41.7f to 120.9f, 39.2f to 119.8f, 37.3f to 117.8f, 36.0f to 115.1f, 35.5f to 112.0f, 36.0f to 108.9f, 37.3f to 106.2f, 39.2f to 104.2f, 41.7f to 103.1f, 44.3f to 103.1f, 46.7f to 104.2f, 48.7f to 106.2f, 50.0f to 108.9f)),
    BodyShape(Muscle.GLUTES, listOf(50.0f to 108.9f, 51.3f to 106.2f, 53.3f to 104.2f, 55.7f to 103.1f, 58.3f to 103.1f, 60.8f to 104.2f, 62.7f to 106.2f, 64.0f to 108.9f, 64.5f to 112.0f, 64.0f to 115.1f, 62.7f to 117.8f, 60.8f to 119.8f, 58.3f to 120.9f, 55.7f to 120.9f, 53.2f to 119.8f, 51.3f to 117.8f, 50.0f to 115.1f, 49.5f to 112.0f)),
    BodyShape(Muscle.HAMSTRINGS, listOf(36f to 122f, 49f to 122f, 47f to 145f, 44f to 160f, 37f to 160f, 35f to 142f)),
    BodyShape(Muscle.HAMSTRINGS, listOf(65f to 142f, 63f to 160f, 56f to 160f, 53f to 145f, 51f to 122f, 64f to 122f)),
    BodyShape(Muscle.CALVES, listOf(44.8f to 182.7f, 44.7f to 187.2f, 44.1f to 191.2f, 43.0f to 194.1f, 41.5f to 195.7f, 39.8f to 195.8f, 38.2f to 194.4f, 36.8f to 191.5f, 35.7f to 187.7f, 35.2f to 183.3f, 35.3f to 178.8f, 35.9f to 174.8f, 37.0f to 171.9f, 38.5f to 170.3f, 40.2f to 170.2f, 41.8f to 171.6f, 43.2f to 174.5f, 44.3f to 178.3f)),
    BodyShape(Muscle.CALVES, listOf(55.7f to 178.3f, 56.8f to 174.5f, 58.2f to 171.6f, 59.8f to 170.2f, 61.5f to 170.3f, 63.0f to 171.9f, 64.1f to 174.8f, 64.7f to 178.8f, 64.8f to 183.3f, 64.3f to 187.7f, 63.2f to 191.5f, 61.8f to 194.4f, 60.2f to 195.8f, 58.5f to 195.7f, 57.0f to 194.1f, 55.9f to 191.2f, 55.3f to 187.2f, 55.2f to 182.7f)),
    BodyShape(null, listOf(36f to 195f, 44f to 195f, 43f to 201f, 37f to 201f)),
    BodyShape(null, listOf(63f to 201f, 57f to 201f, 56f to 195f, 64f to 195f)),
)
