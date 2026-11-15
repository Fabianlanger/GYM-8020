package nl.ibs.gym8020.ui.tour

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import nl.ibs.gym8020.R
import nl.ibs.gym8020.ui.components.Eyebrow
import nl.ibs.gym8020.ui.components.GoButton
import nl.ibs.gym8020.ui.deco.AppEmblem
import nl.ibs.gym8020.ui.deco.DecoDivider
import nl.ibs.gym8020.ui.theme.Gym

/** Test tags for F-11: the offer after the introduction and its two choices. */
const val TOUR_OFFER_TAG = "tour_offer"
const val TOUR_OFFER_START_TAG = "tour_offer_start"
const val TOUR_OFFER_SKIP_TAG = "tour_offer_skip"

/**
 * After the introduction on the first start (#97): a practice workout with a tour, or skip. Offered once; it can be
 * started again from the settings.
 */
@Composable
fun TourOfferScreen(onStart: () -> Unit, onSkip: () -> Unit) {
    BackHandler { onSkip() }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp).testTag(TOUR_OFFER_TAG),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AppEmblem(Modifier.size(96.dp)) }
        Eyebrow(stringResource(R.string.practice_offer_eyebrow), color = Gym.Gold)
        Text(stringResource(R.string.practice_offer_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.practice_offer_body), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.practice_offer_later), style = MaterialTheme.typography.bodySmall, color = Gym.Muted)
        Spacer(Modifier.weight(1f))
        DecoDivider(Modifier.fillMaxWidth())
        GoButton(stringResource(R.string.practice_offer_start), onClick = onStart, modifier = Modifier.testTag(TOUR_OFFER_START_TAG))
        TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally).testTag(TOUR_OFFER_SKIP_TAG)) {
            Text(stringResource(R.string.practice_offer_skip), color = Gym.Muted)
        }
        Spacer(Modifier.height(8.dp))
    }
}
