@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.nav3example.datepicker

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

// ============================================================================
// 1. CORE MODELS
// ============================================================================

/**
 * Selection mode for the date picker.
 */
enum class SelectionMode {
    Single,
    Range,
    Manual
}

/**
 * View mode for the calendar display.
 */
enum class CalendarView {
    Month,
    Week,
    Year
}

/**
 * Represents the result of a date/time selection.
 */
sealed class AppDatePickerResult {
    data class SingleDate(val date: LocalDate) : AppDatePickerResult()
    data class DateRange(val start: LocalDate, val end: LocalDate) : AppDatePickerResult()
    data class SingleDateTime(val dateTime: LocalDateTime) : AppDatePickerResult()
    data class DateTimeRange(
        val start: LocalDateTime,
        val end: LocalDateTime
    ) : AppDatePickerResult()

    data object Empty : AppDatePickerResult()
}

/**
 * Configuration for day item appearance and behavior.
 */
data class DayItemConfig(
    val shape: RoundedCornerShape = CircleShape,
    val selectedColor: Color = Color.Unspecified,
    val todayColor: Color = Color.Unspecified,
    val rangeColor: Color = Color.Unspecified,
    val disabledAlpha: Float = 0.38f,
    val size: Dp = 40.dp,
    val textStyle: TextStyle = TextStyle.Default
)

/**
 * Theming configuration for the entire date picker.
 */
data class DatePickerTheme(
    val backgroundColor: Color = Color.Unspecified,
    val headerColor: Color = Color.Unspecified,
    val headerTextColor: Color = Color.Unspecified,
    val dayItemConfig: DayItemConfig = DayItemConfig(),
    val borderShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    val elevation: Dp = 4.dp
)

// ============================================================================
// 2. DATE PICKER STATE / CONTROLLER
// ============================================================================

/**
 * Returns the start of the week containing [date] for a given [firstDayOfWeek].
 * Declared at top level so it can be used in property initializers.
 */
private fun getWeekStartStatic(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate {
    var d = date
    while (d.dayOfWeek != firstDayOfWeek) {
        d = d.minusDays(1)
    }
    return d
}

/**
 * Controller class that manages the state of the date picker.
 * Use [rememberDatePickerController] to create and remember an instance.
 */
@Stable
class DatePickerController(
    initialDate: LocalDate = LocalDate.now(),
    initialSelectionMode: SelectionMode = SelectionMode.Single,
    initialCalendarView: CalendarView = CalendarView.Month,
    val minDate: LocalDate = LocalDate.of(1900, 1, 1),
    val maxDate: LocalDate = LocalDate.of(2100, 12, 31),
    val showTimePicker: Boolean = false,
    initialFirstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
) {
    // -- Current display state --
    var displayedMonth by mutableStateOf(YearMonth.from(initialDate))
        internal set

    var displayedWeekStart by mutableStateOf(
        getWeekStartStatic(initialDate, initialFirstDayOfWeek)
    )
        internal set

    var calendarView by mutableStateOf(initialCalendarView)

    var selectionMode by mutableStateOf(initialSelectionMode)

    var firstDayOfWeek by mutableStateOf(initialFirstDayOfWeek)

    // -- Selection state --
    var selectedDate by mutableStateOf<LocalDate?>(null)
        internal set

    var rangeStart by mutableStateOf<LocalDate?>(null)
        internal set

    var rangeEnd by mutableStateOf<LocalDate?>(null)
        internal set

    // -- Time state --
    var selectedHour by mutableStateOf(0)
        internal set

    var selectedMinute by mutableStateOf(0)
        internal set

    // -- Manual input --
    var manualInput by mutableStateOf("")
        internal set

    var manualInputError by mutableStateOf<String?>(null)
        internal set

    // -- Derived result --
    val result: AppDatePickerResult
        get() = when (selectionMode) {
            SelectionMode.Single, SelectionMode.Manual -> {
                val date = selectedDate
                if (date != null) {
                    if (showTimePicker) {
                        AppDatePickerResult.SingleDateTime(
                            date.atTime(selectedHour, selectedMinute)
                        )
                    } else {
                        AppDatePickerResult.SingleDate(date)
                    }
                } else AppDatePickerResult.Empty
            }

            SelectionMode.Range -> {
                val start = rangeStart
                val end = rangeEnd
                if (start != null && end != null) {
                    if (showTimePicker) {
                        AppDatePickerResult.DateTimeRange(
                            start.atTime(selectedHour, selectedMinute),
                            end.atTime(23, 59)
                        )
                    } else {
                        AppDatePickerResult.DateRange(start, end)
                    }
                } else AppDatePickerResult.Empty
            }
        }

    // -- Actions --
    fun selectDate(date: LocalDate) {
        if (date.isBefore(minDate) || date.isAfter(maxDate)) return

        when (selectionMode) {
            SelectionMode.Single, SelectionMode.Manual -> {
                selectedDate = date
                manualInput = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                manualInputError = null
            }

            SelectionMode.Range -> {
                when {
                    rangeStart == null -> rangeStart = date
                    rangeEnd == null -> {
                        if (date.isBefore(rangeStart)) {
                            rangeEnd = rangeStart
                            rangeStart = date
                        } else {
                            rangeEnd = date
                        }
                    }

                    else -> {
                        rangeStart = date
                        rangeEnd = null
                    }
                }
            }
        }
    }

    fun parseManualInput(input: String) {
        manualInput = input
        manualInputError = null
        if (input.length == 10) {
            try {
                val parsed = LocalDate.parse(
                    input,
                    DateTimeFormatter.ofPattern("MM/dd/yyyy")
                )
                if (parsed.isBefore(minDate) || parsed.isAfter(maxDate)) {
                    manualInputError = "Date out of allowed range"
                } else {
                    selectedDate = parsed
                    displayedMonth = YearMonth.from(parsed)
                }
            } catch (_: Exception) {
                manualInputError = "Invalid date format (MM/DD/YYYY)"
            }
        }
    }

    fun setTime(hour: Int, minute: Int) {
        selectedHour = hour.coerceIn(0, 23)
        selectedMinute = minute.coerceIn(0, 59)
    }

    fun navigateMonth(forward: Boolean) {
        displayedMonth = if (forward) displayedMonth.plusMonths(1)
        else displayedMonth.minusMonths(1)
    }

    fun navigateWeek(forward: Boolean) {
        displayedWeekStart = if (forward) displayedWeekStart.plusWeeks(1)
        else displayedWeekStart.minusWeeks(1)
    }

    fun navigateYear(forward: Boolean) {
        displayedMonth = if (forward) displayedMonth.plusYears(1)
        else displayedMonth.minusYears(1)
    }

    fun goToDate(date: LocalDate) {
        displayedMonth = YearMonth.from(date)
        displayedWeekStart = getWeekStartStatic(date, firstDayOfWeek)
    }

    fun goToToday() {
        goToDate(LocalDate.now())
    }

    fun clearSelection() {
        selectedDate = null
        rangeStart = null
        rangeEnd = null
        manualInput = ""
        manualInputError = null
        selectedHour = 0
        selectedMinute = 0
    }

    fun isInRange(date: LocalDate): Boolean {
        val s = rangeStart ?: return false
        val e = rangeEnd ?: return false
        return !date.isBefore(s) && !date.isAfter(e)
    }

    fun isRangeStart(date: LocalDate) = rangeStart == date
    fun isRangeEnd(date: LocalDate) = rangeEnd == date
    fun isSelected(date: LocalDate) = selectedDate == date
    fun isDisabled(date: LocalDate) = date.isBefore(minDate) || date.isAfter(maxDate)
}

/**
 * Remember and create a [DatePickerController].
 */
@Composable
fun rememberDatePickerController(
    initialDate: LocalDate = LocalDate.now(),
    selectionMode: SelectionMode = SelectionMode.Single,
    calendarView: CalendarView = CalendarView.Month,
    minDate: LocalDate = LocalDate.of(1900, 1, 1),
    maxDate: LocalDate = LocalDate.of(2100, 12, 31),
    showTimePicker: Boolean = false,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY
): DatePickerController {
    return remember {
        DatePickerController(
            initialDate = initialDate,
            initialSelectionMode = selectionMode,
            initialCalendarView = calendarView,
            minDate = minDate,
            maxDate = maxDate,
            showTimePicker = showTimePicker,
        )
    }
}

// ============================================================================
// 3. DAY SLOT — Customizable from outside
// ============================================================================

/**
 * Data passed to a custom day composable slot.
 */
data class DayState(
    val date: LocalDate,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isInRange: Boolean,
    val isRangeStart: Boolean,
    val isRangeEnd: Boolean,
    val isDisabled: Boolean,
    val isCurrentMonth: Boolean,
    val onClick: () -> Unit
)

/**
 * Default day cell composable. Can be replaced entirely via the `dayContent` slot.
 */
@Composable
fun DefaultDayContent(
    state: DayState,
    config: DayItemConfig = DayItemConfig(),
    colors: DatePickerColors = AppDatePickerDefaults.colors()
) {
    val backgroundColor = when {
        state.isSelected || state.isRangeStart || state.isRangeEnd ->
            if (config.selectedColor != Color.Unspecified) config.selectedColor
            else colors.selectedDayBackground

        state.isInRange ->
            if (config.rangeColor != Color.Unspecified) config.rangeColor
            else colors.rangeDayBackground

        state.isToday ->
            Color.Transparent

        else -> Color.Transparent
    }

    val textColor = when {
        state.isDisabled -> colors.disabledDayText
        state.isSelected || state.isRangeStart || state.isRangeEnd -> colors.selectedDayText
        !state.isCurrentMonth -> colors.otherMonthDayText
        state.isToday -> colors.todayText
        else -> colors.dayText
    }

    val todayBorderColor = if (config.todayColor != Color.Unspecified)
        config.todayColor else colors.todayBorder

    Box(
        modifier = Modifier
            .size(config.size)
            .then(
                if (state.isToday && !state.isSelected && !state.isRangeStart && !state.isRangeEnd) {
                    Modifier.border(1.5.dp, todayBorderColor, config.shape)
                } else Modifier
            )
            .clip(config.shape)
            .background(backgroundColor)
            .clickable(
                enabled = !state.isDisabled,
                indication = ripple(bounded = true),
                interactionSource = remember { MutableInteractionSource() }
            ) { state.onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = state.date.dayOfMonth.toString(),
            style = config.textStyle.copy(
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (state.isToday) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        )
    }
}

// ============================================================================
// 4. COLOR SCHEME
// ============================================================================

data class DatePickerColors(
    val background: Color,
    val headerBackground: Color,
    val headerText: Color,
    val dayText: Color,
    val otherMonthDayText: Color,
    val disabledDayText: Color,
    val selectedDayBackground: Color,
    val selectedDayText: Color,
    val rangeDayBackground: Color,
    val rangeDayText: Color,
    val todayBorder: Color,
    val todayText: Color,
    val weekdayHeaderText: Color,
    val divider: Color,
    val inputBackground: Color,
    val inputText: Color,
    val errorText: Color,
    val iconTint: Color,
    val yearMonthItemBackground: Color,
    val yearMonthSelectedBackground: Color,
    val yearMonthText: Color,
    val yearMonthSelectedText: Color,
    val timePickerBackground: Color,
    val timePickerText: Color,
    val timePickerSelectedBackground: Color,
    val timePickerSelectedText: Color
)

object AppDatePickerDefaults {
    @Composable
    fun colors(
        background: Color = MaterialTheme.colorScheme.surface,
        headerBackground: Color = MaterialTheme.colorScheme.primaryContainer,
        headerText: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        dayText: Color = MaterialTheme.colorScheme.onSurface,
        otherMonthDayText: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledDayText: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
        selectedDayBackground: Color = MaterialTheme.colorScheme.primary,
        selectedDayText: Color = MaterialTheme.colorScheme.onPrimary,
        rangeDayBackground: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        rangeDayText: Color = MaterialTheme.colorScheme.primary,
        todayBorder: Color = MaterialTheme.colorScheme.primary,
        todayText: Color = MaterialTheme.colorScheme.primary,
        weekdayHeaderText: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        divider: Color = MaterialTheme.colorScheme.outlineVariant,
        inputBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
        inputText: Color = MaterialTheme.colorScheme.onSurface,
        errorText: Color = MaterialTheme.colorScheme.error,
        iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        yearMonthItemBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
        yearMonthSelectedBackground: Color = MaterialTheme.colorScheme.primary,
        yearMonthText: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        yearMonthSelectedText: Color = MaterialTheme.colorScheme.onPrimary,
        timePickerBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
        timePickerText: Color = MaterialTheme.colorScheme.onSurface,
        timePickerSelectedBackground: Color = MaterialTheme.colorScheme.primary,
        timePickerSelectedText: Color = MaterialTheme.colorScheme.onPrimary
    ) = DatePickerColors(
        background, headerBackground, headerText, dayText, otherMonthDayText,
        disabledDayText, selectedDayBackground, selectedDayText, rangeDayBackground,
        rangeDayText, todayBorder, todayText, weekdayHeaderText, divider,
        inputBackground, inputText, errorText, iconTint, yearMonthItemBackground,
        yearMonthSelectedBackground, yearMonthText, yearMonthSelectedText,
        timePickerBackground, timePickerText, timePickerSelectedBackground,
        timePickerSelectedText
    )
}

// ============================================================================
// 5. MAIN COMPOSABLE — CompleteDatePicker
// ============================================================================

/**
 * A fully-featured date picker supporting single, range, and manual selection,
 * with month/week/year views, optional time picking, and customizable day slots.
 *
 * @param controller The [DatePickerController] managing state. Use [rememberDatePickerController].
 * @param modifier Modifier for the root container.
 * @param colors Color scheme. Defaults to Material3 theming.
 * @param theme Additional theme configuration.
 * @param dayContent Custom composable slot for rendering each day cell.
 * @param onDateSelected Callback fired whenever the selection changes.
 */
@Composable
fun CompleteDatePicker(
    controller: DatePickerController,
    modifier: Modifier = Modifier,
    colors: DatePickerColors = AppDatePickerDefaults.colors(),
    theme: DatePickerTheme = DatePickerTheme(),
    dayContent: (@Composable (DayState) -> Unit)? = null,
    onDateSelected: ((AppDatePickerResult) -> Unit)? = null
) {
    // Fire callback on result change
    val currentResult = controller.result
    LaunchedEffect(currentResult) {
        if (currentResult !is AppDatePickerResult.Empty) {
            onDateSelected?.invoke(currentResult)
        }
    }

    Surface(
        modifier = modifier,
        shape = theme.borderShape,
        color = if (theme.backgroundColor != Color.Unspecified) theme.backgroundColor
        else colors.background,
        tonalElevation = theme.elevation
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            DatePickerHeader(controller = controller, colors = colors, theme = theme)

            // Mode selector tabs
            ModeSelector(controller = controller, colors = colors)

            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

            // Manual input row (if manual mode)
            AnimatedVisibility(visible = controller.selectionMode == SelectionMode.Manual) {
                ManualDateInput(controller = controller, colors = colors)
            }

            // Calendar views
            AnimatedContent(
                targetState = controller.calendarView,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "CalendarViewTransition"
            ) { view ->
                when (view) {
                    CalendarView.Month -> MonthView(
                        controller = controller,
                        colors = colors,
                        theme = theme,
                        dayContent = dayContent
                    )

                    CalendarView.Week -> WeekView(
                        controller = controller,
                        colors = colors,
                        theme = theme,
                        dayContent = dayContent
                    )

                    CalendarView.Year -> YearView(
                        controller = controller,
                        colors = colors
                    )
                }
            }

            // Time picker
            AnimatedVisibility(visible = controller.showTimePicker) {
                Column {
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    TimePickerSection(controller = controller, colors = colors)
                }
            }
        }
    }
}

// ============================================================================
// 6. HEADER
// ============================================================================

@Composable
private fun DatePickerHeader(
    controller: DatePickerController,
    colors: DatePickerColors,
    theme: DatePickerTheme
) {
    val headerBg = if (theme.headerColor != Color.Unspecified) theme.headerColor
    else colors.headerBackground
    val headerTxt = if (theme.headerTextColor != Color.Unspecified) theme.headerTextColor
    else colors.headerText

    Surface(color = headerBg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // View toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View selector chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CalendarView.entries.forEach { view ->
                        val isActive = controller.calendarView == view
                        FilterChip(
                            selected = isActive,
                            onClick = { controller.calendarView = view },
                            label = {
                                Text(
                                    text = view.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }

                // Today button
                TextButton(onClick = { controller.goToToday() }) {
                    Text("Today", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    when (controller.calendarView) {
                        CalendarView.Month -> controller.navigateMonth(false)
                        CalendarView.Week -> controller.navigateWeek(false)
                        CalendarView.Year -> controller.navigateYear(false)
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        tint = headerTxt
                    )
                }

                Text(
                    text = when (controller.calendarView) {
                        CalendarView.Month -> controller.displayedMonth.format(
                            DateTimeFormatter.ofPattern("MMMM yyyy")
                        )

                        CalendarView.Week -> {
                            val end = controller.displayedWeekStart.plusDays(6)
                            "${controller.displayedWeekStart.format(DateTimeFormatter.ofPattern("MMM d"))} – ${
                                end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                            }"
                        }

                        CalendarView.Year -> controller.displayedMonth.year.toString()
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = headerTxt,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                IconButton(onClick = {
                    when (controller.calendarView) {
                        CalendarView.Month -> controller.navigateMonth(true)
                        CalendarView.Week -> controller.navigateWeek(true)
                        CalendarView.Year -> controller.navigateYear(true)
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = headerTxt
                    )
                }
            }
        }
    }
}

// ============================================================================
// 7. MODE SELECTOR (Single / Range / Manual)
// ============================================================================

@Composable
private fun ModeSelector(
    controller: DatePickerController,
    colors: DatePickerColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectionMode.entries.forEach { mode ->
            val isActive = controller.selectionMode == mode
            val label = when (mode) {
                SelectionMode.Single -> "Single"
                SelectionMode.Range -> "Range"
                SelectionMode.Manual -> "Manual"
            }
            val icon = when (mode) {
                SelectionMode.Single -> Icons.Default.DateRange
                SelectionMode.Range -> Icons.Default.SwapHoriz
                SelectionMode.Manual -> Icons.Default.Edit
            }

            ElevatedFilterChip(
                selected = isActive,
                onClick = {
                    controller.selectionMode = mode
                    controller.clearSelection()
                },
                label = { Text(label, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.height(36.dp)
            )
        }
    }
}

// ============================================================================
// 8. MANUAL DATE INPUT
// ============================================================================

@Composable
private fun ManualDateInput(
    controller: DatePickerController,
    colors: DatePickerColors
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = controller.manualInput,
            onValueChange = { value ->
                // Auto-format: add slashes
                val digits = value.filter { it.isDigit() }
                val formatted = buildString {
                    digits.forEachIndexed { i, c ->
                        if (i == 2 || i == 4) append('/')
                        if (length < 10) append(c)
                    }
                }
                controller.parseManualInput(formatted)
            },
            label = { Text("Date (MM/DD/YYYY)") },
            placeholder = { Text("MM/DD/YYYY") },
            isError = controller.manualInputError != null,
            supportingText = controller.manualInputError?.let {
                { Text(it, color = colors.errorText) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            leadingIcon = {
                Icon(Icons.Default.EditCalendar, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================================
// 9. MONTH VIEW — Pager-based with smooth page-swipe animation
// ============================================================================

/** Virtual page count for the pager. Large enough for ±400 years. */
private const val PAGER_PAGE_COUNT = 10_000
private const val PAGER_INITIAL_PAGE = PAGER_PAGE_COUNT / 2

/**
 * Maps a pager page index to a [YearMonth] relative to a reference month.
 */
private fun pageToYearMonth(page: Int, referenceMonth: YearMonth): YearMonth {
    val offset = (page - PAGER_INITIAL_PAGE).toLong()
    return referenceMonth.plusMonths(offset)
}

/**
 * Maps a [YearMonth] back to a pager page index relative to a reference month.
 */
private fun yearMonthToPage(yearMonth: YearMonth, referenceMonth: YearMonth): Int {
    val diff = yearMonth.year * 12L + yearMonth.monthValue -
            (referenceMonth.year * 12L + referenceMonth.monthValue)
    return (PAGER_INITIAL_PAGE + diff).toInt()
}

@Composable
private fun MonthView(
    controller: DatePickerController,
    colors: DatePickerColors,
    theme: DatePickerTheme,
    dayContent: (@Composable (DayState) -> Unit)?
) {
    // The reference month is captured once; all page offsets are relative to it.
    val referenceMonth = remember { controller.displayedMonth }

    val pagerState = rememberPagerState(
        initialPage = PAGER_INITIAL_PAGE,
        pageCount = { PAGER_PAGE_COUNT }
    )

    // Sync: when pager settles on a new page, update the controller
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val newMonth = pageToYearMonth(pagerState.currentPage, referenceMonth)
            if (newMonth != controller.displayedMonth) {
                controller.displayedMonth = newMonth
            }
        }
    }

    // Sync: when the controller's displayedMonth changes externally (arrows, goToToday),
    // animate the pager to the correct page
    LaunchedEffect(controller.displayedMonth) {
        val targetPage = yearMonthToPage(controller.displayedMonth, referenceMonth)
        if (targetPage != pagerState.currentPage) {
            // Use animate for nearby pages, jump for far-away ones
            val distance = abs(targetPage - pagerState.currentPage)
            if (distance <= 3) {
                pagerState.animateScrollToPage(targetPage)
            } else {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Weekday headers (static — don't scroll with pages)
        WeekdayHeaders(controller.firstDayOfWeek, colors)

        Spacer(modifier = Modifier.height(4.dp))

        // Pager for month grids
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            beyondViewportPageCount = 1,
            key = { it }
        ) { page ->
            val yearMonth = pageToYearMonth(page, referenceMonth)
            MonthGrid(
                yearMonth = yearMonth,
                controller = controller,
                colors = colors,
                theme = theme,
                dayContent = dayContent
            )
        }

        // Selection info
        SelectionInfoRow(controller, colors)
    }
}

/**
 * A single month grid (the content inside each pager page).
 */
@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    controller: DatePickerController,
    colors: DatePickerColors,
    theme: DatePickerTheme,
    dayContent: (@Composable (DayState) -> Unit)?
) {
    val days = getMonthDays(yearMonth, controller.firstDayOfWeek)
    val today = LocalDate.now()

    // Use a simple Column + Row grid instead of LazyVerticalGrid to avoid
    // nested scrollable issues inside the pager.
    Column(modifier = Modifier.fillMaxSize()) {
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                week.forEach { date ->
                    val isCurrentMonth = date.month == yearMonth.month
                            && date.year == yearMonth.year

                    val state = DayState(
                        date = date,
                        isToday = date == today,
                        isSelected = controller.isSelected(date),
                        isInRange = controller.isInRange(date),
                        isRangeStart = controller.isRangeStart(date),
                        isRangeEnd = controller.isRangeEnd(date),
                        isDisabled = controller.isDisabled(date),
                        isCurrentMonth = isCurrentMonth,
                        onClick = { controller.selectDate(date) }
                    )

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // Range background connector
                        if (controller.selectionMode == SelectionMode.Range && state.isInRange) {
                            val rangeConnectorColor = colors.rangeDayBackground
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(theme.dayItemConfig.size)
                                    .drawBehind {
                                        drawRangeConnector(
                                            state = state,
                                            color = rangeConnectorColor
                                        )
                                    }
                            )
                        }

                        // Day content
                        if (dayContent != null) {
                            dayContent(state)
                        } else {
                            DefaultDayContent(
                                state = state,
                                config = theme.dayItemConfig,
                                colors = colors
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws the range connector behind day cells for a seamless range highlight.
 */
private fun DrawScope.drawRangeConnector(state: DayState, color: Color) {
    val y = 0f
    val h = size.height
    when {
        state.isRangeStart && !state.isRangeEnd -> {
            drawRect(color, Offset(size.width / 2, y), Size(size.width / 2, h))
        }

        state.isRangeEnd && !state.isRangeStart -> {
            drawRect(color, Offset(0f, y), Size(size.width / 2, h))
        }

        state.isInRange && !state.isRangeStart && !state.isRangeEnd -> {
            drawRect(color, Offset(0f, y), Size(size.width, h))
        }

        state.isRangeStart && state.isRangeEnd -> { /* single day range, no connector */
        }
    }
}

// ============================================================================
// 10. WEEK VIEW — Pager-based with smooth page-swipe animation
// ============================================================================

/**
 * Maps a pager page index to a week start date relative to a reference date.
 */
private fun pageToWeekStart(page: Int, referenceWeekStart: LocalDate): LocalDate {
    val offset = (page - PAGER_INITIAL_PAGE).toLong()
    return referenceWeekStart.plusWeeks(offset)
}

/**
 * Maps a week start date back to a pager page index.
 */
private fun weekStartToPage(weekStart: LocalDate, referenceWeekStart: LocalDate): Int {
    val days = java.time.temporal.ChronoUnit.DAYS.between(referenceWeekStart, weekStart)
    return (PAGER_INITIAL_PAGE + days / 7).toInt()
}

@Composable
private fun WeekView(
    controller: DatePickerController,
    colors: DatePickerColors,
    theme: DatePickerTheme,
    dayContent: (@Composable (DayState) -> Unit)?
) {
    val referenceWeekStart = remember { controller.displayedWeekStart }

    val pagerState = rememberPagerState(
        initialPage = PAGER_INITIAL_PAGE,
        pageCount = { PAGER_PAGE_COUNT }
    )

    // Sync: pager → controller
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            val newWeekStart = pageToWeekStart(pagerState.currentPage, referenceWeekStart)
            if (newWeekStart != controller.displayedWeekStart) {
                controller.displayedWeekStart = newWeekStart
                // Also keep the month header in sync
                controller.displayedMonth = YearMonth.from(newWeekStart)
            }
        }
    }

    // Sync: controller → pager (for arrow buttons / goToToday)
    LaunchedEffect(controller.displayedWeekStart) {
        val targetPage = weekStartToPage(controller.displayedWeekStart, referenceWeekStart)
        if (targetPage != pagerState.currentPage) {
            val distance = abs(targetPage - pagerState.currentPage)
            if (distance <= 4) {
                pagerState.animateScrollToPage(targetPage)
            } else {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        WeekdayHeaders(controller.firstDayOfWeek, colors)

        Spacer(modifier = Modifier.height(4.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(theme.dayItemConfig.size + 8.dp),
            beyondViewportPageCount = 1,
            key = { it }
        ) { page ->
            val weekStart = pageToWeekStart(page, referenceWeekStart)
            WeekRow(
                weekStart = weekStart,
                controller = controller,
                colors = colors,
                theme = theme,
                dayContent = dayContent
            )
        }

        SelectionInfoRow(controller, colors)
    }
}

/**
 * A single week row (the content inside each pager page).
 */
@Composable
private fun WeekRow(
    weekStart: LocalDate,
    controller: DatePickerController,
    colors: DatePickerColors,
    theme: DatePickerTheme,
    dayContent: (@Composable (DayState) -> Unit)?
) {
    val today = LocalDate.now()
    val weekDays = (0L..6L).map { weekStart.plusDays(it) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDays.forEach { date ->
            val state = DayState(
                date = date,
                isToday = date == today,
                isSelected = controller.isSelected(date),
                isInRange = controller.isInRange(date),
                isRangeStart = controller.isRangeStart(date),
                isRangeEnd = controller.isRangeEnd(date),
                isDisabled = controller.isDisabled(date),
                isCurrentMonth = true,
                onClick = { controller.selectDate(date) }
            )

            if (dayContent != null) {
                dayContent(state)
            } else {
                DefaultDayContent(state = state, config = theme.dayItemConfig, colors = colors)
            }
        }
    }
}

// ============================================================================
// 11. YEAR VIEW
// ============================================================================

@Composable
private fun YearView(
    controller: DatePickerController,
    colors: DatePickerColors
) {
    val today = LocalDate.now()
    val currentYear = controller.displayedMonth.year

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(12) { monthIndex ->
            val month = Month.of(monthIndex + 1)
            val yearMonth = YearMonth.of(currentYear, month)
            val isCurrentMonth = yearMonth == YearMonth.from(today)
            val isDisplayed = yearMonth == controller.displayedMonth

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        controller.displayedMonth = yearMonth
                        controller.calendarView = CalendarView.Month
                    },
                color = when {
                    isDisplayed -> colors.yearMonthSelectedBackground
                    isCurrentMonth -> colors.yearMonthSelectedBackground.copy(alpha = 0.15f)
                    else -> colors.yearMonthItemBackground
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = if (isDisplayed) colors.yearMonthSelectedText
                            else colors.yearMonthText,
                            fontWeight = if (isCurrentMonth || isDisplayed) FontWeight.Bold
                            else FontWeight.Normal
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Mini month preview — show first few days
                    Text(
                        text = "1–${yearMonth.lengthOfMonth()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDisplayed) colors.yearMonthSelectedText.copy(alpha = 0.7f)
                            else colors.yearMonthText.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

// ============================================================================
// 12. TIME PICKER
// ============================================================================

@Composable
private fun TimePickerSection(
    controller: DatePickerController,
    colors: DatePickerColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Time",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = colors.dayText
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour picker
            TimeScrollColumn(
                value = controller.selectedHour,
                range = 0..23,
                onValueChange = { controller.setTime(it, controller.selectedMinute) },
                colors = colors,
                label = "Hr"
            )

            Text(
                ":",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.timePickerText
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Minute picker
            TimeScrollColumn(
                value = controller.selectedMinute,
                range = 0..59,
                onValueChange = { controller.setTime(controller.selectedHour, it) },
                colors = colors,
                label = "Min"
            )
        }

        // Formatted time display
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                controller.selectedHour,
                controller.selectedMinute
            ),
            style = MaterialTheme.typography.labelMedium.copy(color = colors.dayText),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun TimeScrollColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    colors: DatePickerColors,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = colors.timePickerText.copy(alpha = 0.6f)
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Up button
        IconButton(
            onClick = {
                val next = if (value >= range.last) range.first else value + 1
                onValueChange(next)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = colors.iconTint
            )
        }

        // Value display
        Surface(
            color = colors.timePickerSelectedBackground,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%02d", value),
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = colors.timePickerSelectedText,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Down button
        IconButton(
            onClick = {
                val next = if (value <= range.first) range.last else value - 1
                onValueChange(next)
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = colors.iconTint
            )
        }
    }
}

// ============================================================================
// 13. SHARED COMPONENTS
// ============================================================================

@Composable
private fun WeekdayHeaders(firstDayOfWeek: DayOfWeek, colors: DatePickerColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val days = getOrderedDaysOfWeek(firstDayOfWeek)
        days.forEach { day ->
            Text(
                text = day.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                    .take(2)
                    .uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.weekdayHeaderText,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SelectionInfoRow(
    controller: DatePickerController,
    colors: DatePickerColors
) {
    val text = when (controller.selectionMode) {
        SelectionMode.Single, SelectionMode.Manual -> {
            controller.selectedDate?.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
                ?: "No date selected"
        }

        SelectionMode.Range -> {
            val s = controller.rangeStart
            val e = controller.rangeEnd
            when {
                s != null && e != null -> {
                    val fmt = DateTimeFormatter.ofPattern("MMM d")
                    val fmtFull = DateTimeFormatter.ofPattern("MMM d, yyyy")
                    "${s.format(fmt)} – ${e.format(fmtFull)}"
                }

                s != null -> "${s.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))} – …"
                else -> "Select start date"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = colors.dayText.copy(alpha = 0.7f)
            )
        )

        if (controller.result !is AppDatePickerResult.Empty) {
            TextButton(onClick = { controller.clearSelection() }) {
                Text("Clear", fontSize = 12.sp)
            }
        }
    }
}

// ============================================================================
// 14. UTILITIES
// ============================================================================

/**
 * Get all dates to display in a month grid (including overflow from adjacent months).
 */
private fun getMonthDays(yearMonth: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val firstOfMonth = yearMonth.atDay(1)
    val weekFields = WeekFields.of(firstDayOfWeek, 1)

    // Find the first day to display (might be from previous month)
    var start = firstOfMonth.with(weekFields.dayOfWeek(), 1)
    if (start.isAfter(firstOfMonth)) {
        start = start.minusWeeks(1)
    }

    // Always show 6 rows = 42 days for consistent grid height
    return (0L until 42L).map { start.plusDays(it) }
}

/**
 * Returns an ordered list of [DayOfWeek] starting from [firstDayOfWeek].
 */
private fun getOrderedDaysOfWeek(firstDayOfWeek: DayOfWeek): List<DayOfWeek> {
    val allDays = DayOfWeek.entries
    val startIndex = allDays.indexOf(firstDayOfWeek)
    return (0..6).map { allDays[(startIndex + it) % 7] }
}

// ============================================================================
// 15. DIALOG WRAPPER (convenience)
// ============================================================================

/**
 * Shows the [CompleteDatePicker] inside a Material3 dialog.
 */
@Composable
fun AppDatePickerDialog(
    controller: DatePickerController,
    onDismiss: () -> Unit,
    onConfirm: (AppDatePickerResult) -> Unit,
    modifier: Modifier = Modifier,
    colors: DatePickerColors = AppDatePickerDefaults.colors(),
    theme: DatePickerTheme = DatePickerTheme(),
    dayContent: (@Composable (DayState) -> Unit)? = null,
    title: String = "Select Date"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            CompleteDatePicker(
                controller = controller,
                colors = colors,
                theme = theme,
                dayContent = dayContent
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(controller.result) },
                enabled = controller.result !is AppDatePickerResult.Empty
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}