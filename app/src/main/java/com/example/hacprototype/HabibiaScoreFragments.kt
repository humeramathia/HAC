package com.example.hacprototype

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ScoresFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scores, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumePendingMessage()
        view.findViewById<TextView>(R.id.latestScoreText).text =
            HabibiaDummyData.latestScore()?.scoreValue?.toString() ?: "-"
        view.findViewById<TextView>(R.id.averageScoreText).text = HabibiaDummyData.averageScore().toString()
        view.findViewById<TextView>(R.id.highestScoreText).text = HabibiaDummyData.highestScore().toString()

        val practiceCount = HabibiaDummyData.practiceSessions().size
        val leagueCount = HabibiaDummyData.leagueSessions().size
        view.findViewById<TextView>(R.id.practiceHistorySummary).text =
            if (practiceCount == 0) "No practice sessions yet"
            else "$practiceCount practice session${if (practiceCount == 1) "" else "s"} recorded"
        view.findViewById<TextView>(R.id.leagueHistorySummary).text =
            if (leagueCount == 0) "No league rounds yet"
            else "$leagueCount league round${if (leagueCount == 1) "" else "s"} recorded"

        view.findViewById<MaterialCardView>(R.id.practiceChoiceCard).setOnClickListener {
            goTo(PracticeSetupFragment())
        }
        view.findViewById<MaterialCardView>(R.id.leagueChoiceCard).setOnClickListener {
            goTo(LeagueSetupFragment())
        }
        view.findViewById<MaterialCardView>(R.id.practiceHistoryCard).setOnClickListener {
            goTo(PracticeScoresFragment())
        }
        view.findViewById<MaterialCardView>(R.id.leagueHistoryCard).setOnClickListener {
            goTo(LeagueScoresFragment())
        }
        view.findViewById<Button>(R.id.practiceProgressButton).setOnClickListener {
            HabibiaSession.progressType = SessionType.PRACTICE
            goTo(ProgressFragment())
        }
        view.findViewById<Button>(R.id.leagueProgressButton).setOnClickListener {
            HabibiaSession.progressType = SessionType.LEAGUE
            goTo(ProgressFragment())
        }
    }
}

class RecordScoreFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_record_score, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navScores) }
        view.findViewById<MaterialCardView>(R.id.practiceChoiceCard).setOnClickListener {
            goTo(PracticeSetupFragment())
        }
        view.findViewById<MaterialCardView>(R.id.leagueChoiceCard).setOnClickListener {
            goTo(LeagueSetupFragment())
        }
    }
}

class PracticeSetupFragment : Fragment() {
    private var selectedDistance = 18
    private var selectedArrows = 6
    private var selectedRounds = 10

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_practice_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navScores) }

        val distanceGroup = view.findViewById<ChipGroup>(R.id.distanceChipGroup)
        val arrowsGroup = view.findViewById<ChipGroup>(R.id.arrowsChipGroup)
        val roundsGroup = view.findViewById<ChipGroup>(R.id.roundsChipGroup)
        val customDistanceTil = view.findViewById<TextInputLayout>(R.id.customDistanceTil)
        val customArrowsTil = view.findViewById<TextInputLayout>(R.id.customArrowsTil)
        val customRoundsTil = view.findViewById<TextInputLayout>(R.id.customRoundsTil)

        bindChoiceChips(
            group = distanceGroup,
            values = listOf(10, 15, 18, 20, 25, 30, 40, 50, 60, 70),
            selected = selectedDistance,
            label = { "$it m" },
            customLabel = "Custom",
            onSelect = { value, custom ->
                selectedDistance = value
                customDistanceTil.visibility = if (custom) View.VISIBLE else View.GONE
                refreshSummary(view)
            }
        )
        bindChoiceChips(
            group = arrowsGroup,
            values = listOf(3, 6),
            selected = selectedArrows,
            label = { it.toString() },
            customLabel = "Custom",
            onSelect = { value, custom ->
                selectedArrows = value
                customArrowsTil.visibility = if (custom) View.VISIBLE else View.GONE
                refreshSummary(view)
            }
        )
        bindChoiceChips(
            group = roundsGroup,
            values = listOf(5, 8, 10, 12),
            selected = selectedRounds,
            label = { it.toString() },
            customLabel = "Custom",
            onSelect = { value, custom ->
                selectedRounds = value
                customRoundsTil.visibility = if (custom) View.VISIBLE else View.GONE
                refreshSummary(view)
            }
        )

        view.findViewById<TextInputEditText>(R.id.customDistanceInput).setOnFocusChangeListener { _, _ -> refreshSummary(view) }
        view.findViewById<TextInputEditText>(R.id.customArrowsInput).setOnFocusChangeListener { _, _ -> refreshSummary(view) }
        view.findViewById<TextInputEditText>(R.id.customRoundsInput).setOnFocusChangeListener { _, _ -> refreshSummary(view) }
        refreshSummary(view)

        view.findViewById<Button>(R.id.startPracticeButton).setOnClickListener {
            val titleTil = view.findViewById<TextInputLayout>(R.id.titleTil)
            val title = view.findViewById<TextInputEditText>(R.id.titleInput).text?.toString().orEmpty().ifBlank { "Practice" }
            titleTil.error = null
            val distance = readChoice(selectedDistance, view.findViewById(R.id.customDistanceInput), customDistanceTil.visibility == View.VISIBLE)
            val arrows = readChoice(selectedArrows, view.findViewById(R.id.customArrowsInput), customArrowsTil.visibility == View.VISIBLE)
            val rounds = readChoice(selectedRounds, view.findViewById(R.id.customRoundsInput), customRoundsTil.visibility == View.VISIBLE)
            if (distance == null || distance <= 0) {
                showMessage("Enter a valid distance")
                return@setOnClickListener
            }
            if (arrows == null || arrows !in 1..12) {
                showMessage("Arrows per round must be between 1 and 12")
                return@setOnClickListener
            }
            if (rounds == null || rounds !in 1..20) {
                showMessage("Number of rounds must be between 1 and 20")
                return@setOnClickListener
            }
            startDraft(
                type = SessionType.PRACTICE,
                title = title,
                distance = distance,
                arrowsPerEnd = arrows,
                numberOfEnds = rounds,
                date = view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty().ifBlank { "2026-08-30" },
                notes = view.findViewById<TextInputEditText>(R.id.notesInput).text?.toString().orEmpty()
            )
            goTo(ScoreEntryFragment())
        }
    }

    private fun refreshSummary(view: View) {
        val distance = readChoice(selectedDistance, view.findViewById(R.id.customDistanceInput), view.findViewById<View>(R.id.customDistanceTil).visibility == View.VISIBLE) ?: selectedDistance
        val arrows = readChoice(selectedArrows, view.findViewById(R.id.customArrowsInput), view.findViewById<View>(R.id.customArrowsTil).visibility == View.VISIBLE) ?: selectedArrows
        val rounds = readChoice(selectedRounds, view.findViewById(R.id.customRoundsInput), view.findViewById<View>(R.id.customRoundsTil).visibility == View.VISIBLE) ?: selectedRounds
        val max = arrows * rounds * 10
        view.findViewById<TextView>(R.id.setupSummaryText).text =
            "$distance m • $arrows arrows • $rounds rounds • max $max"
    }
}

class LeagueSetupFragment : Fragment() {
    private var selectedDistance = LeagueStandard.DEFAULT_DISTANCE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_league_setup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navScores) }
        view.findViewById<TextView>(R.id.leagueConfigText).text =
            "${LeagueStandard.TOTAL_ARROWS} arrows\n${LeagueStandard.ARROWS_PER_END} arrows per end\n${LeagueStandard.numberOfEnds} ends\nMaximum score ${LeagueStandard.TOTAL_ARROWS * 10}"

        val customDistanceTil = view.findViewById<TextInputLayout>(R.id.customDistanceTil)
        bindChoiceChips(
            group = view.findViewById(R.id.distanceChipGroup),
            values = listOf(10, 15, 18, 20, 25, 30, 40, 50, 60, 70),
            selected = selectedDistance,
            label = { "$it m" },
            customLabel = "Custom",
            onSelect = { value, custom ->
                selectedDistance = value
                customDistanceTil.visibility = if (custom) View.VISIBLE else View.GONE
            }
        )

        view.findViewById<Button>(R.id.startLeagueButton).setOnClickListener {
            val distance = readChoice(
                selectedDistance,
                view.findViewById(R.id.customDistanceInput),
                customDistanceTil.visibility == View.VISIBLE
            )
            if (distance == null || distance <= 0) {
                showMessage("Enter a valid distance")
                return@setOnClickListener
            }
            val title = view.findViewById<TextInputEditText>(R.id.titleInput).text?.toString().orEmpty().ifBlank { "Club League" }
            startDraft(
                type = SessionType.LEAGUE,
                title = title,
                distance = distance,
                arrowsPerEnd = LeagueStandard.ARROWS_PER_END,
                numberOfEnds = LeagueStandard.numberOfEnds,
                date = view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty().ifBlank { "2026-08-30" },
                notes = view.findViewById<TextInputEditText>(R.id.notesInput).text?.toString().orEmpty(),
                leagueName = title
            )
            goTo(ScoreEntryFragment())
        }
    }
}

class ScoreEntryFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_score_entry, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val session = HabibiaSession.draftSession
        if (session == null) {
            openMemberApp(R.id.navScores)
            return
        }
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            if (session.type == SessionType.PRACTICE) goTo(PracticeSetupFragment()) else goTo(LeagueSetupFragment())
        }
        buildKeypad(view.findViewById(R.id.keypadContainer))
        view.findViewById<Button>(R.id.undoArrowButton).setOnClickListener {
            if (HabibiaSession.currentEndArrows.isNotEmpty()) {
                HabibiaSession.currentEndArrows.removeAt(HabibiaSession.currentEndArrows.lastIndex)
                render(view)
            }
        }
        view.findViewById<Button>(R.id.saveRoundButton).setOnClickListener { saveRound(view, session) }
        render(view)
    }

    private fun render(view: View) {
        val session = HabibiaSession.draftSession ?: return
        val currentRound = session.completedEnds + 1
        view.findViewById<TextView>(R.id.entryTitle).text = "${session.typeLabel} – ${session.distanceLabel}"
        view.findViewById<TextView>(R.id.entrySubtitle).text = session.title.uppercase()
        view.findViewById<TextView>(R.id.roundProgressText).text = "Round $currentRound of ${session.numberOfEnds}"
        val arrowsShot = session.arrowsShot + HabibiaSession.currentEndArrows.size
        view.findViewById<TextView>(R.id.arrowsShotText).text = "Arrows shot: $arrowsShot / ${session.totalArrows}"
        val fraction = if (session.totalArrows == 0) 0f else arrowsShot.toFloat() / session.totalArrows
        view.findViewById<TextView>(R.id.progressPercentText).text = "${(fraction * 100).toInt()}%"
        val fill = view.findViewById<View>(R.id.progressFill)
        fill.post {
            val parent = fill.parent as View
            fill.layoutParams = fill.layoutParams.apply {
                width = (parent.width * fraction).toInt().coerceAtLeast(0)
            }
            fill.requestLayout()
        }
        renderSlots(view.findViewById(R.id.arrowSlotsContainer), session.arrowsPerEnd)
        val roundTotal = HabibiaSession.currentEndArrows.sumOf { it.value }
        view.findViewById<TextView>(R.id.roundTotalText).text = roundTotal.toString()
        view.findViewById<TextView>(R.id.sessionTotalText).text =
            "${session.totalScore + roundTotal} / ${session.maxScore}"
    }

    private fun renderSlots(container: LinearLayout, arrowsPerEnd: Int) {
        container.removeAllViews()
        repeat(arrowsPerEnd) { index ->
            val slot = TextView(requireContext()).apply {
                text = HabibiaSession.currentEndArrows.getOrNull(index)?.label ?: "–"
                gravity = Gravity.CENTER
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                val filled = index < HabibiaSession.currentEndArrows.size
                setTextColor(if (filled) Color.WHITE else Color.parseColor("#6B7580"))
                background = ContextCompat.getDrawable(
                    requireContext(),
                    if (filled) R.drawable.bg_score_slot_filled else R.drawable.bg_score_slot
                )
                layoutParams = LinearLayout.LayoutParams(0, 56.dp, 1f).apply {
                    marginStart = 4
                    marginEnd = 4
                }
            }
            container.addView(slot)
        }
    }

    private fun buildKeypad(container: LinearLayout) {
        container.removeAllViews()
        val rows = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("0", "10", "X")
        )
        rows.forEach { row ->
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
            }
            row.forEach { key ->
                val button = TextView(requireContext()).apply {
                    text = key
                    gravity = Gravity.CENTER
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(if (key == "X" || key == "10") Color.parseColor("#7A9A3E") else Color.parseColor("#29333C"))
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_score_key)
                    minHeight = 48.dp
                    layoutParams = LinearLayout.LayoutParams(0, 52.dp, 1f).apply {
                        marginStart = 4
                        marginEnd = 4
                    }
                    setOnClickListener { addArrow(key) }
                }
                rowLayout.addView(button)
            }
            container.addView(rowLayout)
        }
    }

    private fun addArrow(token: String) {
        val session = HabibiaSession.draftSession ?: return
        val error = view?.findViewById<TextView>(R.id.scoreErrorText)
        val parsed = parseArrowInput(token)
        if (parsed == null) {
            error?.text = getString(R.string.invalid_arrow_score)
            error?.visibility = View.VISIBLE
            return
        }
        error?.visibility = View.GONE
        if (HabibiaSession.currentEndArrows.size >= session.arrowsPerEnd) {
            error?.text = "This end is full. Save the round to continue."
            error?.visibility = View.VISIBLE
            return
        }
        HabibiaSession.currentEndArrows.add(parsed)
        view?.let { render(it) }
    }

    private fun saveRound(view: View, session: ScoreSession) {
        val error = view.findViewById<TextView>(R.id.scoreErrorText)
        if (HabibiaSession.currentEndArrows.size != session.arrowsPerEnd) {
            error.text = "Enter a score for every arrow in this end."
            error.visibility = View.VISIBLE
            return
        }
        error.visibility = View.GONE
        session.ends.add(
            ScoreEnd(session.completedEnds + 1, HabibiaSession.currentEndArrows.toMutableList())
        )
        HabibiaSession.currentEndArrows.clear()
        if (session.completedEnds >= session.numberOfEnds) {
            if (session.type == SessionType.LEAGUE && session.ranking == null) {
                session.ranking = 3
                session.fieldSize = 12
            }
            HabibiaDummyData.addCompletedSession(session)
            HabibiaSession.selectedSessionId = session.sessionId
            HabibiaSession.draftSession = null
            HabibiaSession.pendingSnackbar = getString(R.string.session_saved)
            goTo(SessionCompleteFragment())
        } else {
            render(view)
            showMessage("Round ${session.completedEnds} saved")
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}

class SessionCompleteFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_session_complete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val session = HabibiaDummyData.findSession(HabibiaSession.selectedSessionId) ?: return
        val isPractice = session.type == SessionType.PRACTICE
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navScores) }
        view.findViewById<TextView>(R.id.completeHeader).text = if (isPractice) "Practice complete" else "League complete"
        view.findViewById<TextView>(R.id.completeTitle).text =
            if (isPractice) "Practice complete" else "League complete"
        view.findViewById<TextView>(R.id.completeDistance).text = session.distanceLabel.uppercase()
        view.findViewById<TextView>(R.id.completeScore).text = session.scoreLabel
        view.findViewById<View>(R.id.statAverage).bindStat("Average", formatAverage(session.averageArrow))
        view.findViewById<View>(R.id.statArrows).bindStat("Arrows", session.totalArrows.toString())
        view.findViewById<View>(R.id.statTens).bindStat("10s", session.tensCount.toString())
        view.findViewById<View>(R.id.statXs).bindStat("Xs", session.xCount.toString())
        view.findViewById<View>(R.id.statBest).bindStat("Best round", session.highestEnd.toString())
        view.findViewById<View>(R.id.statRounds).bindStat("Rounds", session.numberOfEnds.toString())

        view.findViewById<Button>(R.id.viewDetailsButton).setOnClickListener { goTo(ScoreDetailsFragment()) }
        view.findViewById<Button>(R.id.viewProgressButton).setOnClickListener {
            HabibiaSession.progressType = session.type
            goTo(ProgressFragment())
        }
        view.findViewById<Button>(R.id.backToHistoryButton).apply {
            text = if (isPractice) "Back to Practice Scores" else "View League Scores"
            setOnClickListener {
                if (isPractice) goTo(PracticeScoresFragment()) else goTo(LeagueScoresFragment())
            }
        }
    }
}

class PracticeScoresFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_session_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumePendingMessage()
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navScores) }
        view.findViewById<TextView>(R.id.historyTitle).text = "Practice Scores"
        bindHistory(view.findViewById(R.id.historyContainer), HabibiaDummyData.practiceSessions(), false)
    }
}

class LeagueScoresFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_session_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumePendingMessage()
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navScores) }
        view.findViewById<TextView>(R.id.historyTitle).text = "League Scores"
        bindHistory(view.findViewById(R.id.historyContainer), HabibiaDummyData.leagueSessions(), true)
    }
}

class ScoreDetailsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_score_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val session = HabibiaDummyData.findSession(HabibiaSession.selectedSessionId)
            ?: HabibiaDummyData.findSession(HabibiaSession.selectedScoreId)
            ?: return
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            if (session.type == SessionType.PRACTICE) goTo(PracticeScoresFragment()) else goTo(LeagueScoresFragment())
        }
        val chip = view.findViewById<TextView>(R.id.detailTypeChip)
        chip.text = session.typeLabel
        chip.setChipStyle(
            if (session.type == SessionType.PRACTICE) Color.parseColor("#1A9ABA55") else Color.parseColor("#1A528FD0"),
            if (session.type == SessionType.PRACTICE) Color.parseColor("#7A9A3E") else Color.parseColor("#3E78B8")
        )
        view.findViewById<TextView>(R.id.detailTitle).text = session.title
        view.findViewById<TextView>(R.id.detailMeta).text =
            "${formatDisplayDate(session.date)} • ${session.distanceLabel} • ${session.typeLabel}"
        view.findViewById<TextView>(R.id.detailScore).text = session.scoreLabel
        view.findViewById<View>(R.id.detailAverage).bindStat("Average arrow", formatAverage(session.averageArrow))
        view.findViewById<View>(R.id.detailArrows).bindStat("Total arrows", session.totalArrows.toString())
        view.findViewById<View>(R.id.detailTens).bindStat("10s", session.tensCount.toString())
        view.findViewById<View>(R.id.detailXs).bindStat("Xs", session.xCount.toString())
        view.findViewById<TextView>(R.id.detailNotes).text =
            if (session.notes.isBlank()) "No notes" else session.notes

        val container = view.findViewById<LinearLayout>(R.id.endsContainer)
        container.removeAllViews()
        session.ends.forEach { end ->
            val item = layoutInflater.inflate(R.layout.item_end_row, container, false)
            item.findViewById<TextView>(R.id.endTitle).text = "Round ${end.endNumber}"
            item.findViewById<TextView>(R.id.endArrows).text = end.arrowLabels()
            item.findViewById<TextView>(R.id.endTotal).text = "Total: ${end.total}"
            container.addView(item)
        }
    }
}

class ProgressFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_score_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val type = HabibiaSession.progressType
        val sessions = if (type == SessionType.PRACTICE) {
            HabibiaDummyData.practiceSessions()
        } else {
            HabibiaDummyData.leagueSessions()
        }.sortedBy { it.date }
        val isPractice = type == SessionType.PRACTICE
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navScores) }
        view.findViewById<TextView>(R.id.progressTitle).text = if (isPractice) "Practice Progress" else "League Progress"
        view.findViewById<TextView>(R.id.chartHeading).text = if (isPractice) "Practice Progress" else "League Performance"

        val average = if (sessions.isEmpty()) 0.0 else sessions.map { it.averageArrow }.average()
        val best = sessions.maxOfOrNull { it.totalScore } ?: 0
        val latest = sessions.maxByOrNull { it.date }?.totalScore ?: 0
        view.findViewById<View>(R.id.statAverage).bindStat(
            if (isPractice) "Practice average" else "League average",
            formatAverage(average)
        )
        view.findViewById<View>(R.id.statBest).bindStat(
            if (isPractice) "Best practice" else "Best league score",
            best.toString()
        )
        view.findViewById<View>(R.id.statLatest).bindStat(
            if (isPractice) "Latest practice" else "Latest league score",
            latest.toString()
        )
        view.findViewById<View>(R.id.statImprovement).bindStat(
            "Improvement",
            formatImprovement(HabibiaDummyData.sessionImprovement(type))
        )
        view.findViewById<TextView>(R.id.trendLabel).text =
            if (isPractice) "Practice scores over recent sessions" else "League scores by month"

        val container = view.findViewById<LinearLayout>(R.id.progressChartContainer)
        container.removeAllViews()
        if (sessions.isEmpty()) return
        val max = sessions.maxOf { it.totalScore }.coerceAtLeast(1)
        sessions.forEach { session ->
            val col = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    marginEnd = 8
                }
            }
            val value = TextView(requireContext()).apply {
                text = session.totalScore.toString()
                textSize = 11f
                setTextColor(Color.parseColor("#6B7580"))
                gravity = Gravity.CENTER
            }
            val bar = View(requireContext())
            val height = ((session.totalScore.toFloat() / max) * 150f).toInt().coerceAtLeast(16)
            bar.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            bar.background = GradientDrawable().apply {
                cornerRadius = 10f
                colors = intArrayOf(Color.parseColor("#9ABA55"), Color.parseColor("#528FD0"))
                orientation = GradientDrawable.Orientation.BOTTOM_TOP
            }
            val label = TextView(requireContext()).apply {
                text = if (isPractice) session.date.takeLast(2) else monthLabel(session.date)
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#6B7580"))
            }
            col.addView(value)
            col.addView(bar)
            col.addView(label)
            container.addView(col)
        }
    }
}

private fun startDraft(
    type: SessionType,
    title: String,
    distance: Int,
    arrowsPerEnd: Int,
    numberOfEnds: Int,
    date: String,
    notes: String,
    leagueName: String? = null
) {
    HabibiaSession.currentEndArrows.clear()
    HabibiaSession.draftSession = ScoreSession(
        sessionId = "S${System.currentTimeMillis()}",
        memberId = HabibiaDummyData.member.memberId,
        type = type,
        title = title,
        distanceMeters = distance,
        arrowsPerEnd = arrowsPerEnd,
        numberOfEnds = numberOfEnds,
        date = date,
        notes = notes,
        ends = mutableListOf(),
        leagueName = leagueName
    )
}

private fun Fragment.bindHistory(container: LinearLayout, sessions: List<ScoreSession>, showRank: Boolean) {
    container.removeAllViews()
    if (sessions.isEmpty()) {
        container.bindEmptyState(
            getString(if (showRank) R.string.empty_league_title else R.string.empty_practice_title),
            getString(if (showRank) R.string.empty_league_body else R.string.empty_practice_body)
        )
        return
    }
    sessions.forEach { session ->
        val item = layoutInflater.inflate(R.layout.item_session_card, container, false)
        item.findViewById<TextView>(R.id.sessionTitle).text = "${session.typeLabel} – ${session.distanceLabel}"
        item.findViewById<TextView>(R.id.sessionDate).text = formatDisplayDate(session.date)
        item.findViewById<TextView>(R.id.sessionScore).text = session.scoreLabel
        item.findViewById<TextView>(R.id.sessionAverage).text = formatAverage(session.averageArrow)
        val rank = item.findViewById<TextView>(R.id.sessionRank)
        if (showRank && session.ranking != null) {
            rank.visibility = View.VISIBLE
            rank.text = "${session.ranking}${ordinal(session.ranking!!)} / ${session.fieldSize ?: 12}"
        } else {
            rank.visibility = View.GONE
        }
        item.findViewById<TextView>(R.id.sessionMeta).text = buildString {
            append("${session.totalArrows} arrows")
            if (showRank) append("  •  ${session.tensCount} 10s  •  ${session.xCount} Xs")
            if (session.title.isNotBlank()) append("  •  ${session.title}")
        }
        val open = {
            HabibiaSession.selectedSessionId = session.sessionId
            goTo(ScoreDetailsFragment())
        }
        item.setOnClickListener { open() }
        item.findViewById<Button>(R.id.viewDetailsButton).setOnClickListener { open() }
        container.addView(item)
    }
}

private fun bindChoiceChips(
    group: ChipGroup,
    values: List<Int>,
    selected: Int,
    label: (Int) -> String,
    customLabel: String,
    onSelect: (Int, Boolean) -> Unit
) {
    group.removeAllViews()
    values.forEach { value ->
        val chip = Chip(group.context).apply {
            text = label(value)
            isCheckable = true
            isChecked = value == selected
            tag = value
            setOnClickListener { onSelect(value, false) }
        }
        group.addView(chip)
    }
    val custom = Chip(group.context).apply {
        text = customLabel
        isCheckable = true
        tag = -1
        setOnClickListener { onSelect(selected, true) }
    }
    group.addView(custom)
    group.setOnCheckedStateChangeListener { chipGroup, checkedIds ->
        val chip = checkedIds.firstOrNull()?.let { chipGroup.findViewById<Chip>(it) }
        val value = chip?.tag as? Int ?: return@setOnCheckedStateChangeListener
        onSelect(if (value == -1) selected else value, value == -1)
    }
}

private fun readChoice(selected: Int, input: TextInputEditText, custom: Boolean): Int? {
    if (!custom) return selected
    return input.text?.toString()?.toIntOrNull()
}

private fun parseArrowInput(token: String): ArrowScore? {
    val cleaned = token.trim()
    if (cleaned.equals("X", ignoreCase = true)) return ArrowScore(10, true)
    val value = cleaned.toIntOrNull() ?: return null
    if (value !in 0..10) return null
    return ArrowScore(value, false)
}

private fun ordinal(value: Int): String {
    return when {
        value % 100 in 11..13 -> "th"
        value % 10 == 1 -> "st"
        value % 10 == 2 -> "nd"
        value % 10 == 3 -> "rd"
        else -> "th"
    }
}

private fun monthLabel(iso: String): String {
    val month = iso.split("-").getOrNull(1)?.toIntOrNull() ?: return iso
    return listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        .getOrNull(month - 1) ?: iso
}
