package io.github.estebangocoalt.blocdenotas.ui.common

import androidx.annotation.StringRes
import io.github.estebangocoalt.blocdenotas.R
import io.github.estebangocoalt.blocdenotas.data.Aisle

@StringRes
fun Aisle.labelRes(): Int = when (this) {
    Aisle.PRODUCE -> R.string.aisle_produce
    Aisle.BAKERY -> R.string.aisle_bakery
    Aisle.MEAT -> R.string.aisle_meat
    Aisle.FISH -> R.string.aisle_fish
    Aisle.DAIRY -> R.string.aisle_dairy
    Aisle.PANTRY -> R.string.aisle_pantry
    Aisle.DRINKS -> R.string.aisle_drinks
    Aisle.FROZEN -> R.string.aisle_frozen
    Aisle.CLEANING -> R.string.aisle_cleaning
    Aisle.HYGIENE -> R.string.aisle_hygiene
    Aisle.OTHER -> R.string.aisle_other
}
