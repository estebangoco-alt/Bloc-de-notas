package io.github.estebangocoalt.blocdenotas.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.HolidayRegion

@StringRes
fun HolidayRegion.labelRes(): Int = when (this) {
    HolidayRegion.NONE -> R.string.holidays_none
    HolidayRegion.SPAIN -> R.string.holidays_spain
    HolidayRegion.ANDALUCIA -> R.string.holidays_andalucia
    HolidayRegion.ARAGON -> R.string.holidays_aragon
    HolidayRegion.ASTURIAS -> R.string.holidays_asturias
    HolidayRegion.BALEARES -> R.string.holidays_baleares
    HolidayRegion.CANARIAS -> R.string.holidays_canarias
    HolidayRegion.CANTABRIA -> R.string.holidays_cantabria
    HolidayRegion.CASTILLA_LA_MANCHA -> R.string.holidays_clm
    HolidayRegion.CASTILLA_Y_LEON -> R.string.holidays_cyl
    HolidayRegion.CATALONIA -> R.string.holidays_catalonia
    HolidayRegion.EXTREMADURA -> R.string.holidays_extremadura
    HolidayRegion.GALICIA -> R.string.holidays_galicia
    HolidayRegion.MADRID -> R.string.holidays_madrid
    HolidayRegion.MURCIA -> R.string.holidays_murcia
    HolidayRegion.NAVARRA -> R.string.holidays_navarra
    HolidayRegion.PAIS_VASCO -> R.string.holidays_pais_vasco
    HolidayRegion.LA_RIOJA -> R.string.holidays_la_rioja
    HolidayRegion.VALENCIA -> R.string.holidays_valencia
    HolidayRegion.CEUTA -> R.string.holidays_ceuta
    HolidayRegion.MELILLA -> R.string.holidays_melilla
}

/** Lista con todas las zonas de festivos para elegir una. */
@Composable
fun HolidayRegionDialog(
    selected: HolidayRegion,
    onSelect: (HolidayRegion) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_holidays)) },
        text = {
            LazyColumn {
                items(HolidayRegion.entries.toList(), key = { it.name }) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null, modifier = Modifier.padding(12.dp))
                        Text(stringResource(option.labelRes()), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
