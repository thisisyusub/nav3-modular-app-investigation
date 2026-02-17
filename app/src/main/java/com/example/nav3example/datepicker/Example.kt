package com.example.nav3example.datepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate

// ============================================================================
// EXAMPLE 1: Basic Single Date Selection
// ============================================================================

@Composable
fun BasicSingleDatePickerExample() {
    val controller = rememberDatePickerController(
        selectionMode = SelectionMode.Range
    )

    var result by remember { mutableStateOf<AppDatePickerResult>(AppDatePickerResult.Empty) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Basic Single Date Picker", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        CompleteDatePicker(
            controller = controller,
            onDateSelected = { result = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Read the result
        when (val r = result) {
            is AppDatePickerResult.SingleDate -> Text("Selected: ${r.date}")
            is AppDatePickerResult.Empty -> Text("No date selected")
            else -> {}
        }
    }
}

// ============================================================================
// EXAMPLE 2: Range Selection with Min/Max Dates
// ============================================================================

@Composable
fun RangeDatePickerExample() {
    val controller = rememberDatePickerController(
        selectionMode = SelectionMode.Range,
        minDate = LocalDate.now(),
        maxDate = LocalDate.now().plusMonths(6)
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Range Picker (next 6 months only)", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        CompleteDatePicker(
            controller = controller,
            onDateSelected = { result ->
                when (result) {
                    is AppDatePickerResult.DateRange -> {
                        println("Range: ${result.start} to ${result.end}")
                    }
                    else -> {}
                }
            }
        )
    }
}

// ============================================================================
// EXAMPLE 3: Manual Input Mode
// ============================================================================

@Composable
fun ManualInputExample() {
    val controller = rememberDatePickerController(
        selectionMode = SelectionMode.Manual
    )

    CompleteDatePicker(controller = controller)
}

// ============================================================================
// EXAMPLE 4: Date + Time Picker
// ============================================================================

@Composable
fun DateTimePickerExample() {
    val controller = rememberDatePickerController(
        selectionMode = SelectionMode.Single,
        showTimePicker = true
    )

    CompleteDatePicker(
        controller = controller,
        onDateSelected = { result ->
            when (result) {
                is AppDatePickerResult.SingleDateTime -> {
                    println("Selected: ${result.dateTime}")
                }
                else -> {}
            }
        }
    )
}

// ============================================================================
// EXAMPLE 5: Custom Day Slots (fully custom rendering)
// ============================================================================

@Composable
fun CustomDaySlotExample() {
    val controller = rememberDatePickerController()

    // Example: highlight specific "event" dates
    val eventDates = remember {
        setOf(
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(7),
            LocalDate.now().plusDays(14),
        )
    }

    CompleteDatePicker(
        controller = controller,
        dayContent = { state ->
            // Completely custom day rendering
            val hasEvent = state.date in eventDates
            val bgColor = when {
                state.isSelected -> Color(0xFF6200EE)
                hasEvent -> Color(0xFFFF9800).copy(alpha = 0.2f)
                else -> Color.Transparent
            }
            val textColor = when {
                state.isDisabled -> Color.Gray.copy(alpha = 0.4f)
                state.isSelected -> Color.White
                !state.isCurrentMonth -> Color.Gray.copy(alpha = 0.4f)
                else -> Color.DarkGray
            }

            Column(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable(enabled = !state.isDisabled) { state.onClick() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.date.dayOfMonth.toString(),
                    fontSize = 13.sp,
                    color = textColor,
                    fontWeight = if (state.isToday) FontWeight.ExtraBold else FontWeight.Normal
                )
                if (hasEvent) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFF9800))
                    )
                }
            }
        }
    )
}

// ============================================================================
// EXAMPLE 6: Custom Theme & Colors
// ============================================================================

@Composable
fun CustomThemeExample() {
    val controller = rememberDatePickerController()

    CompleteDatePicker(
        controller = controller,
        colors = AppDatePickerDefaults.colors(
            background = Color(0xFF1A1A2E),
            headerBackground = Color(0xFF16213E),
            headerText = Color(0xFFE94560),
            dayText = Color(0xFFEEEEEE),
            selectedDayBackground = Color(0xFFE94560),
            selectedDayText = Color.White,
            rangeDayBackground = Color(0xFFE94560).copy(alpha = 0.2f),
            todayBorder = Color(0xFFE94560),
            todayText = Color(0xFFE94560),
            weekdayHeaderText = Color(0xFFAAAAAA),
            divider = Color(0xFF333333),
            iconTint = Color(0xFFE94560),
            yearMonthItemBackground = Color(0xFF16213E),
            yearMonthSelectedBackground = Color(0xFFE94560),
            yearMonthText = Color(0xFFCCCCCC),
            yearMonthSelectedText = Color.White,
            timePickerBackground = Color(0xFF16213E),
            timePickerText = Color(0xFFEEEEEE),
            timePickerSelectedBackground = Color(0xFFE94560),
            timePickerSelectedText = Color.White
        ),
        theme = DatePickerTheme(
            borderShape = RoundedCornerShape(24.dp),
            elevation = 8.dp,
            dayItemConfig = DayItemConfig(
                shape = RoundedCornerShape(12.dp),
                size = 42.dp
            )
        )
    )
}

// ============================================================================
// EXAMPLE 7: Week View Starting on Sunday
// ============================================================================

@Composable
fun WeekViewSundayStartExample() {
    val controller = rememberDatePickerController(
        calendarView = CalendarView.Week,
        firstDayOfWeek = DayOfWeek.SUNDAY
    )

    CompleteDatePicker(controller = controller)
}

// ============================================================================
// EXAMPLE 8: Year View
// ============================================================================

@Composable
fun YearViewExample() {
    val controller = rememberDatePickerController(
        calendarView = CalendarView.Year
    )

    CompleteDatePicker(controller = controller)
}

// ============================================================================
// EXAMPLE 9: Dialog Usage
// ============================================================================

@Composable
fun DialogExample() {
    var showDialog by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf("No date selected") }

    val controller = rememberDatePickerController(
        selectionMode = SelectionMode.Range,
        showTimePicker = true
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { showDialog = true }) {
            Text("Open Date Picker Dialog")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(selectedText)
    }

    if (showDialog) {
        AppDatePickerDialog(
            controller = controller,
            onDismiss = { showDialog = false },
            onConfirm = { result ->
                selectedText = when (result) {
                    is AppDatePickerResult.SingleDate -> "Date: ${result.date}"
                    is AppDatePickerResult.DateRange -> "Range: ${result.start} → ${result.end}"
                    is AppDatePickerResult.SingleDateTime -> "DateTime: ${result.dateTime}"
                    is AppDatePickerResult.DateTimeRange -> "Range: ${result.start} → ${result.end}"
                    AppDatePickerResult.Empty -> "Nothing selected"
                }
                showDialog = false
            },
            title = "Pick a Date Range"
        )
    }
}

// ============================================================================
// EXAMPLE 10: Controller-Based Observation
// ============================================================================

@Composable
fun ControllerObservationExample() {
    val controller = rememberDatePickerController(
        selectionMode = SelectionMode.Single,
        showTimePicker = true,
        minDate = LocalDate.of(2025, 1, 1),
        maxDate = LocalDate.of(2025, 12, 31)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // The picker
        CompleteDatePicker(controller = controller)

        Spacer(modifier = Modifier.height(16.dp))

        // Observe all state changes reactively
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Controller State", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Text("Mode: ${controller.selectionMode}")
                Text("View: ${controller.calendarView}")
                Text("Displayed Month: ${controller.displayedMonth}")
                Text("Selected Date: ${controller.selectedDate ?: "none"}")
                Text("Range: ${controller.rangeStart ?: "—"} to ${controller.rangeEnd ?: "—"}")
                Text("Time: ${String.format("%02d:%02d", controller.selectedHour, controller.selectedMinute)}")
                Text("Result: ${controller.result}")

                Spacer(modifier = Modifier.height(8.dp))

                // Programmatic control
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { controller.goToToday() }) {
                        Text("Today")
                    }
                    Button(onClick = { controller.clearSelection() }) {
                        Text("Clear")
                    }
                    Button(onClick = {
                        controller.selectDate(LocalDate.of(2025, 6, 15))
                    }) {
                        Text("Select Jun 15")
                    }
                }
            }
        }
    }
}