# Stillgood

**Is it still good? Your fridge, in eat-by order.**

Stillgood is a leftovers log. When you put food away, tap a preset (Leftovers, Pizza, Cooked rice,
Raw chicken…) and save. The list stays sorted by "eat by" date and color-coded, so the container at the
back of the fridge doesn't get forgotten. When you finish something, mark it **Ate it** or **Tossed it**
and Stillgood keeps a small "% saved" tally, a gentle nudge against food waste.

## Features (POC)

- One-tap presets with typical fridge times based on USDA FoodKeeper guidance; days are editable (1–30)
- "Went in the fridge" today / yesterday / 2 days ago, with a live "Eat by Saturday, Oct 21" preview
- List sorted soonest first, with green / amber / orange / red status ("3 days", "Tomorrow", "Eat today", "Past date")
- Summary line ("5 items · 2 to eat today") and an eaten-vs-tossed tally
- Edit or remove any item; light and dark theme; TalkBack descriptions on every row

## Why it's low risk

- **No permissions** and no network, so it can't leak anything and costs nothing to run
- **No third-party libraries**: Kotlin and the Android framework only
- Dates are stored as plain day numbers (`LocalDate.toEpochDay()`), which avoids time-zone surprises
- A clear "guide only, when in doubt throw it out" disclaimer on the list and add screens

## Run it

**Android Studio:** File → Open → this `stillgood` folder → Gradle sync → pick or create an emulator
(API 36) → Run ▶.

**Command line** (with `ANDROID_HOME` set or a `local.properties` with `sdk.dir=...`):

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
.\gradlew.bat test
```

## Project layout

```
app/src/main/java/com/georgewitte/stillgood/
  MainActivity.kt       the fridge list, ate/tossed/edit actions, tally
  AddFoodActivity.kt    presets, name, keeps-for stepper, added-on day, eat-by preview
  ui/Ui.kt              edge-to-edge insets + drawing helpers
  data/FoodStore.kt     SharedPreferences + JSON persistence
  logic/Food.kt         model, presets, status, sorting, tally (unit tested)
```

## Build setup

AGP 9.2.1 (built-in Kotlin) · Gradle 9.4.1 · compileSdk/targetSdk 36 · minSdk 26 · JDK 17+.

## Before publishing to Google Play

- Confirm the final `applicationId`, create an upload key, build with `.\gradlew.bat bundleRelease`
- Data safety form: **no data collected or shared**
- Keep the food-safety wording as guidance ("rough guide", "when in doubt, throw it out"), not a promise

## Next ideas

- Local "eat today" reminder each morning (AlarmManager + notification, still no server)
- Home-screen widget listing the next three items to eat
- Freezer mode with longer presets
