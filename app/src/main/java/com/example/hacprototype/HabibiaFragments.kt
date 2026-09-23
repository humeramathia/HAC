// ========================================
// START OF CODE
// ========================================

package com.example.hacprototype

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject

/** Branded splash; [MainActivity] replaces it with login after a short delay. */
class SplashFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }
}

/**
 * Email/password sign-in against `/auth/login`.
 *
 * Demo Member uses the seeded API account. Demo Admin skips login on purpose
 * and opens the admin dashboard without a Firebase token.
 */
class LoginFragment : Fragment() {
    private var passwordVisible = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.passwordInput)
        val emailError = view.findViewById<TextView>(R.id.emailError)
        val passwordError = view.findViewById<TextView>(R.id.passwordError)
        val passwordToggle = view.findViewById<ImageButton>(R.id.passwordToggle)

        passwordToggle.setOnClickListener {
            passwordVisible = !passwordVisible
            if (passwordVisible) {
                passwordInput.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle.setImageResource(R.drawable.ic_visibility_off)
            } else {
                passwordInput.inputType =
                    android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setImageResource(R.drawable.ic_visibility)
            }
            passwordInput.setSelection(passwordInput.text?.length ?: 0)
        }

        view.findViewById<View>(R.id.loginButton).setOnClickListener {
            val email = emailInput.text?.toString().orEmpty()
            val password = passwordInput.text?.toString().orEmpty()
            emailError.visibility = View.GONE
            passwordError.visibility = View.GONE
            var valid = true
            if (email.isBlank()) {
                emailError.text = "Email is required"
                emailError.visibility = View.VISIBLE
                valid = false
            }
            if (password.isBlank()) {
                passwordError.text = "Password is required"
                passwordError.visibility = View.VISIBLE
                valid = false
            }
            if (!valid) return@setOnClickListener
            signInThenOpen(email, password)
        }
        view.findViewById<View>(R.id.registerButton).setOnClickListener { goTo(RegisterFragment()) }
        view.findViewById<View>(R.id.demoMemberButton).setOnClickListener {
            signInThenOpen("member@habibia.co.za", "Member123")
        }
        // Intentionally no API call — Demo Admin is a local shortcut.
        view.findViewById<View>(R.id.demoAdminButton).setOnClickListener { openAdminApp() }
    }

    private fun signInThenOpen(email: String, password: String) {
        val loginButton = view?.findViewById<View>(R.id.loginButton)
        val passwordError = view?.findViewById<TextView>(R.id.passwordError)
        loginButton?.isEnabled = false
        passwordError?.visibility = View.GONE
        apiInBackground(
            work = {
                HabibiaApi.post(
                    "/auth/login",
                    JSONObject().put("email", email).put("password", password).toString(),
                    token = null
                )
            },
            onError = { message ->
                loginButton?.isEnabled = true
                if (message.contains("verify your email", ignoreCase = true)) {
                    HabibiaSession.pendingEmail = email
                    HabibiaSession.pendingPassword = password
                    goTo(EmailVerificationFragment())
                    return@apiInBackground
                }
                passwordError?.text = message
                passwordError?.visibility = View.VISIBLE
            },
            onOk = { json ->
                loginButton?.isEnabled = true
                applyAuthSuccess(json)
            }
        )
    }
}

/** Posts `/auth/register` then holds credentials for the verification screen. */
class RegisterFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.registerButton).setOnClickListener {
            val first = view.findViewById<TextInputEditText>(R.id.firstNameInput).text?.toString().orEmpty()
            val last = view.findViewById<TextInputEditText>(R.id.lastNameInput).text?.toString().orEmpty()
            val email = view.findViewById<TextInputEditText>(R.id.emailInput).text?.toString().orEmpty()
            val password = view.findViewById<TextInputEditText>(R.id.passwordInput).text?.toString().orEmpty()
            val confirm = view.findViewById<TextInputEditText>(R.id.confirmPasswordInput).text?.toString().orEmpty()
            if (first.isBlank() || last.isBlank() || email.isBlank() || password.isBlank()) {
                showMessage("Please complete all fields")
                return@setOnClickListener
            }
            if (password != confirm) {
                showMessage("Passwords do not match")
                return@setOnClickListener
            }
            val button = view.findViewById<Button>(R.id.registerButton)
            button.isEnabled = false
            apiInBackground(
                work = {
                    HabibiaApi.post(
                        "/auth/register",
                        JSONObject()
                            .put("firstName", first)
                            .put("lastName", last)
                            .put("email", email)
                            .put("password", password)
                            .toString(),
                        token = null
                    )
                },
                onError = { message ->
                    button.isEnabled = true
                    showMessage(message)
                },
                onOk = {
                    HabibiaSession.pendingEmail = email
                    HabibiaSession.pendingPassword = password
                    HabibiaDummyData.member = HabibiaDummyData.member.copy(
                        firstName = first,
                        lastName = last,
                        email = email
                    )
                    goTo(EmailVerificationFragment())
                }
            )
        }
        view.findViewById<Button>(R.id.backToLoginButton).setOnClickListener { goTo(LoginFragment()) }
    }
}

/**
 * After register or unverified login: resend the Firebase email, then confirm
 * via `/auth/confirm-verification` once the user has clicked the mail link.
 */
class EmailVerificationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_email_verification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val email = HabibiaSession.pendingEmail.orEmpty()
        val password = HabibiaSession.pendingPassword.orEmpty()
        view.findViewById<TextView>(R.id.verifyEmailText).text = email.ifBlank { "your email address" }
        view.findViewById<Button>(R.id.backToLoginButton).setOnClickListener { goTo(LoginFragment()) }

        view.findViewById<Button>(R.id.verifyButton).setOnClickListener {
            if (email.isBlank() || password.isBlank()) {
                showMessage("Please register or log in first")
                return@setOnClickListener
            }
            val button = view.findViewById<Button>(R.id.verifyButton)
            button.isEnabled = false
            apiInBackground(
                work = {
                    HabibiaApi.post(
                        "/auth/resend-verification",
                        JSONObject().put("email", email).put("password", password).toString(),
                        token = null
                    )
                },
                onError = { message ->
                    button.isEnabled = true
                    showMessage(message)
                },
                onOk = { json ->
                    button.isEnabled = true
                    val already = JSONObject(json).optBoolean("alreadyVerified")
                    if (already) {
                        showMessage("Email already verified. You can continue.")
                    } else {
                        showMessage("Verification email sent. Check your inbox.")
                    }
                }
            )
        }

        view.findViewById<Button>(R.id.continueButton).setOnClickListener {
            if (email.isBlank() || password.isBlank()) {
                showMessage("Please register or log in first")
                return@setOnClickListener
            }
            val button = view.findViewById<Button>(R.id.continueButton)
            button.isEnabled = false
            apiInBackground(
                work = {
                    HabibiaApi.post(
                        "/auth/confirm-verification",
                        JSONObject().put("email", email).put("password", password).toString(),
                        token = null
                    )
                },
                onError = { message ->
                    button.isEnabled = true
                    showMessage(message)
                },
                onOk = { json ->
                    applyAuthSuccess(json)
                }
            )
        }
    }
}

/** Member shell: bottom nav plus a child fragment for the selected tab. */
class MemberHostFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_member_host, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.memberBottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            HabibiaSession.selectedMemberTab = item.itemId
            showTab(item.itemId)
            true
        }
        bottomNav.selectedItemId = HabibiaSession.selectedMemberTab
        showTab(HabibiaSession.selectedMemberTab)
        consumePendingMessage()
    }

    private fun showTab(itemId: Int) {
        val fragment = when (itemId) {
            R.id.navCalendar -> ClubCalendarFragment()
            R.id.navScores -> ScoresFragment()
            R.id.navResources -> BeginnerResourcesFragment()
            R.id.navProfile -> ProfileFragment()
            else -> MemberDashboardFragment()
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.memberTabContainer, fragment)
            .commit()
    }
}

/**
 * Member home.
 *
 * Featured event and score tiles still use leftover [HabibiaDummyData].
 * The unread notification dot is loaded from `/notifications`.
 */
class MemberDashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_member_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val member = HabibiaDummyData.member
        view.findViewById<TextView>(R.id.greetingText).text =
            "${HabibiaDummyData.greeting()}, ${member.firstName}"

        view.findViewById<View>(R.id.notificationDot).visibility = View.GONE
        // Trailing lambda is onOk (see [apiInBackground]); unread state is live API data.
        apiInBackground({ parseNotificationList(HabibiaApi.get("/notifications")) }) { notifications ->
            if (!isAdded) return@apiInBackground
            view.findViewById<View>(R.id.notificationDot).visibility =
                if (notifications.any { !it.isRead }) View.VISIBLE else View.GONE
        }

        val event = HabibiaDummyData.nextEvent()
        if (event != null) {
            view.findViewById<TextView>(R.id.featuredEventTitle).text = event.title
            view.findViewById<TextView>(R.id.featuredEventDate).text = formatDisplayDate(event.eventDate)
            view.findViewById<TextView>(R.id.featuredEventTime).text = event.eventTime
            view.findViewById<TextView>(R.id.featuredEventLocation).text = event.location
            view.findViewById<MaterialCardView>(R.id.featuredEventCard).setOnClickListener {
                HabibiaSession.selectedEventId = event.eventId
                goTo(EventDetailsFragment())
            }
        }

        view.findViewById<TextView>(R.id.latestScoreText).text =
            HabibiaDummyData.latestScore()?.scoreValue?.toString() ?: "-"
        view.findViewById<TextView>(R.id.averageScoreText).text =
            HabibiaDummyData.averageScore().toString()
        view.findViewById<TextView>(R.id.highestScoreText).text =
            HabibiaDummyData.highestScore().toString()
        renderMiniChart(view.findViewById(R.id.miniChartContainer))

        val competition = HabibiaDummyData.nextCompetition()
        if (competition != null) {
            view.findViewById<TextView>(R.id.competitionNameText).text = competition.competitionName
            view.findViewById<TextView>(R.id.competitionDateText).text = formatDisplayDate(competition.competitionDate)
            view.findViewById<TextView>(R.id.competitionVenueText).text = competition.venue
            view.findViewById<Button>(R.id.viewCompetitionButton).setOnClickListener {
                HabibiaSession.selectedCompetitionId = competition.competitionId
                goTo(CompetitionDetailsFragment())
            }
            view.findViewById<MaterialCardView>(R.id.upcomingCompetitionCard).setOnClickListener {
                HabibiaSession.selectedCompetitionId = competition.competitionId
                goTo(CompetitionDetailsFragment())
            }
        }

        view.findViewById<ImageButton>(R.id.notificationButton).setOnClickListener { goTo(NotificationFragment()) }
        view.findViewById<ImageButton>(R.id.profileButton).setOnClickListener {
            HabibiaSession.selectedMemberTab = R.id.navProfile
            openMemberApp(R.id.navProfile)
        }
        view.findViewById<Button>(R.id.viewProgressButton).setOnClickListener { goTo(ProgressFragment()) }
        view.findViewById<Button>(R.id.recordScoreButton).setOnClickListener { goTo(RecordScoreFragment()) }
        view.findViewById<Button>(R.id.calendarButton).setOnClickListener { openMemberApp(R.id.navCalendar) }
        view.findViewById<Button>(R.id.competitionsButton).setOnClickListener { goTo(CompetitionFragment()) }
        view.findViewById<Button>(R.id.resourcesButton).setOnClickListener { openMemberApp(R.id.navResources) }
    }

    private fun renderMiniChart(container: LinearLayout) {
        container.removeAllViews()
        val scores = HabibiaDummyData.memberSessions().sortedBy { it.date }.takeLast(5)
        if (scores.isEmpty()) return
        val max = scores.maxOf { it.totalScore }.coerceAtLeast(1)
        scores.forEach { score ->
            val bar = View(requireContext())
            val height = ((score.totalScore.toFloat() / max) * 48f).toInt().coerceAtLeast(8)
            val params = LinearLayout.LayoutParams(0, height, 1f)
            params.marginEnd = 6
            bar.layoutParams = params
            bar.background = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(Color.parseColor("#9ABA55"))
            }
            container.addView(bar)
        }
    }
}

/** Combined event and competition list from the API, filtered by type chips. */
class ClubCalendarFragment : Fragment() {
    private var filter = "All"
    private var query = ""
    private var events = emptyList<ClubEvent>()
    private var competitions = emptyList<Competition>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_club_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextInputEditText>(R.id.searchEventsInput).addTextChangedListener(simpleWatcher {
            query = it
            render(view)
        })
        view.findViewById<ChipGroup>(R.id.eventFilterGroup).setOnCheckedStateChangeListener { _, checkedIds ->
            filter = when (checkedIds.firstOrNull()) {
                R.id.filterPractice -> "Practice"
                R.id.filterEvents -> "Event"
                R.id.filterCompetitions -> "Competition"
                else -> "All"
            }
            render(view)
        }
        apiInBackground({
            parseEventList(HabibiaApi.get("/events")) to parseCompetitionList(HabibiaApi.get("/competitions"))
        }) { (loadedEvents, loadedCompetitions) ->
            if (!isAdded) return@apiInBackground
            events = loadedEvents
            competitions = loadedCompetitions
            render(view)
        }
    }

    private fun render(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.eventsListContainer)
        container.removeAllViews()
        val eventItems = events.filter {
            val typeMatch = when (filter) {
                "All" -> true
                "Practice" -> it.type.equals("Practice", true)
                "Event" -> it.type.equals("Event", true)
                else -> false
            }
            typeMatch && (query.isBlank() || it.title.contains(query, true) || it.location.contains(query, true))
        }
        val competitionItems = if (filter == "All" || filter == "Competition") {
            competitions.filter {
                query.isBlank() || it.competitionName.contains(query, true) || it.venue.contains(query, true)
            }
        } else emptyList()

        if (eventItems.isEmpty() && competitionItems.isEmpty()) {
            container.bindEmptyState(getString(R.string.empty_events_title), getString(R.string.empty_events_body))
            return
        }

        eventItems.forEach { event ->
            val item = layoutInflater.inflate(R.layout.item_event_card, container, false)
            val chip = item.findViewById<TextView>(R.id.eventTypeChip)
            chip.text = event.type.uppercase()
            chip.setChipStyle(
                if (event.type.equals("Practice", true)) Color.parseColor("#1A9ABA55") else Color.parseColor("#1A528FD0"),
                if (event.type.equals("Practice", true)) Color.parseColor("#7A9A3E") else Color.parseColor("#528FD0")
            )
            item.findViewById<TextView>(R.id.eventTitle).text = event.title
            item.findViewById<TextView>(R.id.eventDate).text = formatDisplayDate(event.eventDate)
            item.findViewById<TextView>(R.id.eventTime).text = event.eventTime
            item.findViewById<TextView>(R.id.eventLocation).text = event.location
            item.setOnClickListener {
                HabibiaSession.selectedEventId = event.eventId
                goTo(EventDetailsFragment())
            }
            container.addView(item)
        }

        competitionItems.forEach { competition ->
            val item = layoutInflater.inflate(R.layout.item_event_card, container, false)
            val chip = item.findViewById<TextView>(R.id.eventTypeChip)
            chip.text = "COMPETITION"
            chip.setChipStyle(Color.parseColor("#1AFE3B3A"), Color.parseColor("#FE3B3A"))
            item.findViewById<TextView>(R.id.eventTitle).text = competition.competitionName
            item.findViewById<TextView>(R.id.eventDate).text = formatDisplayDate(competition.competitionDate)
            item.findViewById<TextView>(R.id.eventTime).text = "All day"
            item.findViewById<TextView>(R.id.eventLocation).text = competition.venue
            item.setOnClickListener {
                HabibiaSession.selectedCompetitionId = competition.competitionId
                goTo(CompetitionDetailsFragment())
            }
            container.addView(item)
        }
    }
}

/** Detail for [HabibiaSession.selectedEventId]; falls back to the first event. */
class EventDetailsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_event_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navCalendar) }
        apiInBackground({ parseEventList(HabibiaApi.get("/events")) }) { events ->
            if (!isAdded) return@apiInBackground
            val event = events.find { it.eventId == HabibiaSession.selectedEventId } ?: events.firstOrNull()
            if (event == null) return@apiInBackground
            view.findViewById<TextView>(R.id.eventTitle).text = event.title
            view.findViewById<TextView>(R.id.eventDate).text = formatDisplayDate(event.eventDate)
            view.findViewById<TextView>(R.id.eventTime).text = event.eventTime
            view.findViewById<TextView>(R.id.eventLocation).text = event.location
            view.findViewById<TextView>(R.id.eventDescription).text = event.description
            view.findViewById<Button>(R.id.primaryActionButton).setOnClickListener {
                showMessage("Added to your club plan")
            }
        }
    }
}

/** Member competition list loaded from `/competitions`. */
class CompetitionFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_competition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val container = view.findViewById<LinearLayout>(R.id.competitionsListContainer)
        var competitions = emptyList<Competition>()
        fun render(query: String) {
            container.removeAllViews()
            val items = competitions.filter {
                query.isBlank() || it.competitionName.contains(query, true) || it.venue.contains(query, true)
            }
            if (items.isEmpty()) {
                container.bindEmptyState(getString(R.string.empty_competitions_title), getString(R.string.empty_competitions_body))
                return
            }
            items.forEach { competition ->
                val item = layoutInflater.inflate(R.layout.item_competition_card, container, false)
                val status = item.findViewById<TextView>(R.id.statusChip)
                status.text = competition.status
                status.setChipStyle(competitionStatusColor(competition.status), competitionStatusTextColor(competition.status))
                item.findViewById<TextView>(R.id.competitionName).text = competition.competitionName
                item.findViewById<TextView>(R.id.competitionDate).text = formatDisplayDate(competition.competitionDate)
                item.findViewById<TextView>(R.id.venueText).text = competition.venue
                item.findViewById<TextView>(R.id.deadlineText).text =
                    "Register by ${formatDisplayDate(competition.registrationDeadline)}"
                item.setOnClickListener {
                    HabibiaSession.selectedCompetitionId = competition.competitionId
                    goTo(CompetitionDetailsFragment())
                }
                container.addView(item)
            }
        }
        view.findViewById<TextInputEditText>(R.id.searchCompetitionsInput).addTextChangedListener(simpleWatcher { render(it) })
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp() }
        apiInBackground({ parseCompetitionList(HabibiaApi.get("/competitions")) }) { loaded ->
            if (!isAdded) return@apiInBackground
            competitions = loaded
            render(view.findViewById<TextInputEditText>(R.id.searchCompetitionsInput).text?.toString().orEmpty())
        }
    }
}

/** Competition detail; back returns to admin manage or the member list. */
class CompetitionDetailsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_competition_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            if (HabibiaSession.isAdmin) goTo(ManageCompetitionsFragment()) else goTo(CompetitionFragment())
        }
        apiInBackground({ parseCompetitionList(HabibiaApi.get("/competitions")) }) { competitions ->
            if (!isAdded) return@apiInBackground
            val competition = competitions.find { it.competitionId == HabibiaSession.selectedCompetitionId }
                ?: competitions.firstOrNull() ?: return@apiInBackground
            val status = view.findViewById<TextView>(R.id.statusChip)
            status.text = competition.status
            status.setChipStyle(competitionStatusColor(competition.status), competitionStatusTextColor(competition.status))
            view.findViewById<TextView>(R.id.competitionName).text = competition.competitionName
            view.findViewById<TextView>(R.id.competitionDate).text = formatDisplayDate(competition.competitionDate)
            view.findViewById<TextView>(R.id.venueText).text = competition.venue
            view.findViewById<TextView>(R.id.deadlineText).text =
                "Registration deadline: ${formatDisplayDate(competition.registrationDeadline)}"
            view.findViewById<TextView>(R.id.descriptionText).text = competition.description
            view.findViewById<Button>(R.id.primaryActionButton).setOnClickListener {
                showMessage("Registration interest saved")
            }
        }
    }
}

/** Read-only club announcements (edit/delete hidden; those live in admin). */
class AnnouncementFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_announcement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp() }
        val container = view.findViewById<LinearLayout>(R.id.announcementsListContainer)
        apiInBackground({ parseAnnouncementList(HabibiaApi.get("/announcements")) }) { announcements ->
            if (!isAdded) return@apiInBackground
            container.removeAllViews()
            if (announcements.isEmpty()) {
                container.bindEmptyState("No announcements", "Club updates will appear here.")
                return@apiInBackground
            }
            announcements.forEach { announcement ->
                val item = layoutInflater.inflate(R.layout.item_manage_row, container, false)
                item.findViewById<TextView>(R.id.titleText).text = announcement.title
                item.findViewById<TextView>(R.id.subtitleText).text =
                    "${announcement.content}\n${formatDisplayDate(announcement.datePosted)}"
                item.findViewById<Button>(R.id.editButton).visibility = View.GONE
                item.findViewById<Button>(R.id.deleteButton).visibility = View.GONE
                container.addView(item)
            }
        }
    }
}

/** Inbox from `/notifications`; tapping an unread row marks it read on the API. */
class NotificationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp() }
        val container = view.findViewById<LinearLayout>(R.id.notificationsListContainer)
        var notifications = emptyList<Notification>()
        fun render() {
            container.removeAllViews()
            if (notifications.isEmpty()) {
                container.bindEmptyState(
                    getString(R.string.empty_notifications_title),
                    getString(R.string.empty_notifications_body)
                )
                return
            }
            notifications.forEach { notification ->
                val item = layoutInflater.inflate(R.layout.item_notification_row, container, false)
                item.findViewById<View>(R.id.unreadDot).visibility =
                    if (notification.isRead) View.INVISIBLE else View.VISIBLE
                item.findViewById<TextView>(R.id.notificationTitle).text = notification.title
                item.findViewById<TextView>(R.id.notificationMessage).text = notification.message
                item.findViewById<TextView>(R.id.notificationDate).text = notification.dateSent
                if (!notification.isRead) {
                    item.setBackgroundColor(Color.parseColor("#14FE3B3A"))
                }
                item.setOnClickListener {
                    if (!notification.isRead) {
                        apiInBackground({
                            HabibiaApi.put("/notifications/${notification.notificationId}/read", "{}")
                        }) {
                            notification.isRead = true
                            render()
                        }
                    }
                }
                container.addView(item)
            }
        }
        apiInBackground({ parseNotificationList(HabibiaApi.get("/notifications")) }) { loaded ->
            if (!isAdded) return@apiInBackground
            notifications = loaded
            render()
        }
    }
}

/** Beginner resources from `/resources`, filtered by category chip. */
class BeginnerResourcesFragment : Fragment() {
    private var category = "All"
    private var query = ""
    private var resources = emptyList<BeginnerResource>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_beginner_resources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextInputEditText>(R.id.searchResourcesInput).addTextChangedListener(simpleWatcher {
            query = it
            render(view)
        })
        view.findViewById<ChipGroup>(R.id.resourceCategoryGroup).setOnCheckedStateChangeListener { _, checkedIds ->
            category = when (checkedIds.firstOrNull()) {
                R.id.catSafety -> "Safety"
                R.id.catEquipment -> "Equipment"
                R.id.catTechnique -> "Technique"
                R.id.catScoring -> "Scoring"
                R.id.catStarted -> "Getting Started"
                else -> "All"
            }
            render(view)
        }
        apiInBackground({ parseResourceList(HabibiaApi.get("/resources")) }) { loaded ->
            if (!isAdded) return@apiInBackground
            resources = loaded
            render(view)
        }
    }

    private fun render(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.resourcesListContainer)
        container.removeAllViews()
        val items = resources.filter {
            (category == "All" || it.category.equals(category, true)) &&
                (query.isBlank() || it.title.contains(query, true) || it.category.contains(query, true))
        }
        if (items.isEmpty()) {
            container.bindEmptyState(getString(R.string.empty_resources_title), getString(R.string.empty_resources_body))
            return
        }
        items.forEach { resource ->
            val item = layoutInflater.inflate(R.layout.item_resource_card, container, false)
            item.findViewById<TextView>(R.id.resourceCategory).text = resource.category
            item.findViewById<TextView>(R.id.resourceTitle).text = resource.title
            item.findViewById<TextView>(R.id.resourceDescription).text = resource.description
            item.setOnClickListener {
                HabibiaSession.selectedResourceId = resource.resourceId
                goTo(ResourceDetailsFragment())
            }
            container.addView(item)
        }
    }
}

/** One resource; the open-link action is a toast, not an external browser. */
class ResourceDetailsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_resource_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navResources) }
        apiInBackground({ parseResourceList(HabibiaApi.get("/resources")) }) { resources ->
            if (!isAdded) return@apiInBackground
            val resource = resources.find { it.resourceId == HabibiaSession.selectedResourceId }
                ?: resources.firstOrNull() ?: return@apiInBackground
            view.findViewById<TextView>(R.id.resourceCategory).text = resource.category
            view.findViewById<TextView>(R.id.resourceTitle).text = resource.title
            view.findViewById<TextView>(R.id.resourceDescription).text = resource.description
            view.findViewById<Button>(R.id.openLinkButton).setOnClickListener {
                showMessage("Opening: ${resource.resourceLink}")
            }
        }
    }
}

/**
 * Signed-in profile. Dummy values paint first so the screen is not blank
 * while `/me` and `/me/profile` load.
 */
class ProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumePendingMessage()
        bindProfile(view, HabibiaDummyData.member, HabibiaDummyData.profile)
        apiInBackground({
            parseClubMember(JSONObject(HabibiaApi.get("/me"))) to
                parseMemberProfile(JSONObject(HabibiaApi.get("/me/profile")))
        }) { (member, profile) ->
            if (!isAdded) return@apiInBackground
            HabibiaDummyData.member = member
            HabibiaDummyData.profile = profile
            bindProfile(view, member, profile)
        }
        view.findViewById<Button>(R.id.editProfileButton).setOnClickListener { goTo(EditProfileFragment()) }
        view.findViewById<Button>(R.id.settingsButton).setOnClickListener {
            showMessage("Settings are available in a future release")
        }
        view.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            val token = HabibiaSession.authToken
            if (!token.isNullOrBlank()) {
                Thread {
                    try {
                        HabibiaApi.post("/auth/logout", "{}", token)
                    } catch (_: Exception) {
                    }
                }.start()
            }
            HabibiaSession.clearAuth()
            goTo(LoginFragment())
        }
    }
}

/** PUT `/me/profile` then queues a snackbar for the profile tab. */
class EditProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val member = HabibiaDummyData.member
        val profile = HabibiaDummyData.profile
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navProfile) }
        view.findViewById<TextInputEditText>(R.id.firstNameInput).setText(member.firstName)
        view.findViewById<TextInputEditText>(R.id.lastNameInput).setText(member.lastName)
        view.findViewById<TextInputEditText>(R.id.emailInput).setText(member.email)
        view.findViewById<TextInputEditText>(R.id.experienceInput).setText(profile.experienceLevel)
        view.findViewById<TextInputEditText>(R.id.bowTypeInput).setText(profile.bowType)
        view.findViewById<TextInputEditText>(R.id.divisionInput).setText(profile.division)
        view.findViewById<TextInputEditText>(R.id.emergencyInput).setText(profile.emergencyContact)
        view.findViewById<Button>(R.id.saveProfileButton).setOnClickListener {
            val first = view.findViewById<TextInputEditText>(R.id.firstNameInput).text?.toString().orEmpty()
            val last = view.findViewById<TextInputEditText>(R.id.lastNameInput).text?.toString().orEmpty()
            if (first.isBlank() || last.isBlank()) {
                showMessage("Name fields are required")
                return@setOnClickListener
            }
            val saveButton = view.findViewById<Button>(R.id.saveProfileButton)
            saveButton.isEnabled = false
            apiInBackground(
                work = {
                    HabibiaApi.put(
                        "/me/profile",
                        JSONObject()
                            .put("firstName", first)
                            .put("lastName", last)
                            .put("email", view.findViewById<TextInputEditText>(R.id.emailInput).text?.toString().orEmpty())
                            .put("experienceLevel", view.findViewById<TextInputEditText>(R.id.experienceInput).text?.toString().orEmpty())
                            .put("bowType", view.findViewById<TextInputEditText>(R.id.bowTypeInput).text?.toString().orEmpty())
                            .put("division", view.findViewById<TextInputEditText>(R.id.divisionInput).text?.toString().orEmpty())
                            .put("emergencyContact", view.findViewById<TextInputEditText>(R.id.emergencyInput).text?.toString().orEmpty())
                            .toString()
                    )
                },
                onError = { message ->
                    saveButton.isEnabled = true
                    showMessage(message)
                },
                onOk = { json ->
                    val body = JSONObject(json)
                    body.optJSONObject("member")?.let { HabibiaDummyData.member = parseClubMember(it) }
                    body.optJSONObject("profile")?.let { HabibiaDummyData.profile = parseMemberProfile(it) }
                    HabibiaSession.pendingSnackbar = getString(R.string.profile_updated)
                    openMemberApp(R.id.navProfile)
                }
            )
        }
    }
}

/** Admin home: live counts plus shortcuts. Demo Admin lands here without auth. */
class AdminDashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        HabibiaSession.isAdmin = true
        consumePendingMessage()
        apiInBackground({
            Triple(
                parseClubMembers(HabibiaApi.get("/admin/members")).size,
                parseEventList(HabibiaApi.get("/events")).size,
                parseCompetitionList(HabibiaApi.get("/competitions")).size
            )
        }) { (memberCount, eventCount, competitionCount) ->
            if (!isAdded) return@apiInBackground
            view.findViewById<TextView>(R.id.totalMembersText).text = memberCount.toString()
            view.findViewById<TextView>(R.id.upcomingEventsText).text = eventCount.toString()
            view.findViewById<TextView>(R.id.upcomingCompetitionsText).text = competitionCount.toString()
        }
        view.findViewById<Button>(R.id.memberProgressButton).setOnClickListener { goTo(AdminMemberListFragment()) }
        view.findViewById<Button>(R.id.manageMembersButton).setOnClickListener { goTo(ManageMembersFragment()) }
        view.findViewById<Button>(R.id.manageEventsButton).setOnClickListener { goTo(ManageEventsFragment()) }
        view.findViewById<Button>(R.id.manageCompetitionsButton).setOnClickListener { goTo(ManageCompetitionsFragment()) }
        view.findViewById<Button>(R.id.manageAnnouncementsButton).setOnClickListener { goTo(ManageAnnouncementsFragment()) }
        view.findViewById<Button>(R.id.manageResourcesButton).setOnClickListener { goTo(ManageResourcesFragment()) }
        view.findViewById<Button>(R.id.statisticsButton).setOnClickListener { goTo(AdminStatisticsFragment()) }
        view.findViewById<Button>(R.id.logoutAdminButton).setOnClickListener {
            val token = HabibiaSession.authToken
            if (!token.isNullOrBlank()) {
                Thread {
                    try {
                        HabibiaApi.post("/auth/logout", "{}", token)
                    } catch (_: Exception) {
                    }
                }.start()
            }
            HabibiaSession.clearAuth()
            goTo(LoginFragment())
        }
    }
}

/** Admin member list. Admin accounts cannot be deleted or opened for scores. */
class ManageMembersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_members, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(AdminDashboardFragment()) }
        val container = view.findViewById<LinearLayout>(R.id.membersListContainer)
        var members = emptyList<Member>()
        fun render(query: String) {
            container.removeAllViews()
            members.filter {
                query.isBlank() || it.fullName.contains(query, true) || it.email.contains(query, true)
            }.forEach { member ->
                val item = layoutInflater.inflate(R.layout.item_manage_row, container, false)
                item.findViewById<TextView>(R.id.titleText).text = member.fullName
                item.findViewById<TextView>(R.id.subtitleText).text = "${member.role} • ${member.email}"
                item.findViewById<Button>(R.id.editButton).text = "Progress"
                item.findViewById<Button>(R.id.editButton).setOnClickListener {
                    if (member.role == "Admin") {
                        showMessage("Admin accounts have no score history")
                        return@setOnClickListener
                    }
                    HabibiaSession.selectedMemberId = member.memberId
                    goTo(AdminMemberProgressFragment())
                }
                item.findViewById<Button>(R.id.deleteButton).setOnClickListener {
                    if (member.role == "Admin") {
                        showMessage("Cannot delete admin account")
                        return@setOnClickListener
                    }
                    confirmDelete {
                        apiInBackground({ HabibiaApi.delete("/admin/members/${member.memberId}") }) {
                            members = members.filter { it.memberId != member.memberId }
                            showMessage("Member deleted")
                            render(query)
                        }
                    }
                }
                container.addView(item)
            }
        }
        view.findViewById<TextInputEditText>(R.id.searchMembersInput).addTextChangedListener(simpleWatcher { render(it) })
        apiInBackground({ parseClubMembers(HabibiaApi.get("/admin/members")) }) { loaded ->
            if (!isAdded) return@apiInBackground
            members = loaded
            render(view.findViewById<TextInputEditText>(R.id.searchMembersInput).text?.toString().orEmpty())
        }
    }
}

/** Admin event list; null [HabibiaSession.editingEventId] means create. */
class ManageEventsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_events, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumePendingMessage()
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(AdminDashboardFragment()) }
        view.findViewById<Button>(R.id.addEventButton).setOnClickListener {
            HabibiaSession.editingEventId = null
            goTo(AddEditEventFragment())
        }
        val container = view.findViewById<LinearLayout>(R.id.eventsListContainer)
        var events = emptyList<ClubEvent>()
        fun render(query: String) {
            container.removeAllViews()
            events.filter {
                query.isBlank() || it.title.contains(query, true) || it.location.contains(query, true)
            }.forEach { event ->
                val item = layoutInflater.inflate(R.layout.item_manage_row, container, false)
                item.findViewById<TextView>(R.id.titleText).text = event.title
                item.findViewById<TextView>(R.id.subtitleText).text =
                    "${formatDisplayDate(event.eventDate)} • ${event.eventTime} • ${event.location}"
                item.findViewById<Button>(R.id.editButton).setOnClickListener {
                    HabibiaSession.editingEventId = event.eventId
                    goTo(AddEditEventFragment())
                }
                item.findViewById<Button>(R.id.deleteButton).setOnClickListener {
                    confirmDelete {
                        apiInBackground({ HabibiaApi.delete("/events/${event.eventId}") }) {
                            events = events.filter { it.eventId != event.eventId }
                            showMessage(getString(R.string.event_deleted))
                            render(query)
                        }
                    }
                }
                container.addView(item)
            }
        }
        view.findViewById<TextInputEditText>(R.id.searchEventsInput).addTextChangedListener(simpleWatcher { render(it) })
        apiInBackground({ parseEventList(HabibiaApi.get("/events")) }) { loaded ->
            if (!isAdded) return@apiInBackground
            events = loaded
            render(view.findViewById<TextInputEditText>(R.id.searchEventsInput).text?.toString().orEmpty())
        }
    }
}

/** Create (`POST /events`) or update (`PUT /events/{id}`) from session edit id. */
class AddEditEventFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_edit_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editingId = HabibiaSession.editingEventId
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(ManageEventsFragment()) }
        if (!editingId.isNullOrBlank()) {
            apiInBackground({ parseEventList(HabibiaApi.get("/events")) }) { events ->
                if (!isAdded) return@apiInBackground
                val existing = events.find { it.eventId == editingId } ?: return@apiInBackground
                view.findViewById<TextInputEditText>(R.id.titleInput).setText(existing.title)
                view.findViewById<TextInputEditText>(R.id.dateInput).setText(existing.eventDate)
                view.findViewById<TextInputEditText>(R.id.timeInput).setText(existing.eventTime)
                view.findViewById<TextInputEditText>(R.id.locationInput).setText(existing.location)
                view.findViewById<TextInputEditText>(R.id.descriptionInput).setText(existing.description)
                view.findViewById<TextInputEditText>(R.id.typeInput).setText(existing.type)
            }
        }
        view.findViewById<Button>(R.id.saveEventButton).setOnClickListener {
            val title = view.findViewById<TextInputEditText>(R.id.titleInput).text?.toString().orEmpty()
            if (title.isBlank()) {
                showMessage("Title is required")
                return@setOnClickListener
            }
            val body = JSONObject()
                .put("title", title)
                .put("description", view.findViewById<TextInputEditText>(R.id.descriptionInput).text?.toString().orEmpty())
                .put("eventDate", view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty())
                .put("eventTime", view.findViewById<TextInputEditText>(R.id.timeInput).text?.toString().orEmpty())
                .put("location", view.findViewById<TextInputEditText>(R.id.locationInput).text?.toString().orEmpty())
                .put("type", view.findViewById<TextInputEditText>(R.id.typeInput).text?.toString().orEmpty().ifBlank { "Event" })
                .toString()
            val saveButton = view.findViewById<Button>(R.id.saveEventButton)
            saveButton.isEnabled = false
            apiInBackground(
                work = {
                    if (editingId.isNullOrBlank()) HabibiaApi.post("/events", body)
                    else HabibiaApi.put("/events/$editingId", body)
                },
                onError = { message ->
                    saveButton.isEnabled = true
                    showMessage(message)
                },
                onOk = {
                    HabibiaSession.pendingSnackbar = getString(R.string.event_saved)
                    goTo(ManageEventsFragment())
                }
            )
        }
    }
}

/** Admin competition list. */
class ManageCompetitionsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_competitions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumePendingMessage()
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(AdminDashboardFragment()) }
        view.findViewById<Button>(R.id.addCompetitionButton).setOnClickListener {
            HabibiaSession.editingCompetitionId = null
            goTo(AddEditCompetitionFragment())
        }
        val container = view.findViewById<LinearLayout>(R.id.competitionsListContainer)
        var competitions = emptyList<Competition>()
        fun render(query: String) {
            container.removeAllViews()
            competitions.filter {
                query.isBlank() || it.competitionName.contains(query, true)
            }.forEach { competition ->
                val item = layoutInflater.inflate(R.layout.item_manage_row, container, false)
                item.findViewById<TextView>(R.id.titleText).text = competition.competitionName
                item.findViewById<TextView>(R.id.subtitleText).text =
                    "${competition.status} • ${formatDisplayDate(competition.competitionDate)}"
                item.findViewById<Button>(R.id.editButton).setOnClickListener {
                    HabibiaSession.editingCompetitionId = competition.competitionId
                    goTo(AddEditCompetitionFragment())
                }
                item.findViewById<Button>(R.id.deleteButton).setOnClickListener {
                    confirmDelete {
                        apiInBackground({ HabibiaApi.delete("/competitions/${competition.competitionId}") }) {
                            competitions = competitions.filter { it.competitionId != competition.competitionId }
                            showMessage(getString(R.string.competition_deleted))
                            render(query)
                        }
                    }
                }
                container.addView(item)
            }
        }
        view.findViewById<TextInputEditText>(R.id.searchCompetitionsInput).addTextChangedListener(simpleWatcher { render(it) })
        apiInBackground({ parseCompetitionList(HabibiaApi.get("/competitions")) }) { loaded ->
            if (!isAdded) return@apiInBackground
            competitions = loaded
            render(view.findViewById<TextInputEditText>(R.id.searchCompetitionsInput).text?.toString().orEmpty())
        }
    }
}

/** Create or update a competition; blank status defaults to UPCOMING. */
class AddEditCompetitionFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_edit_competition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editingId = HabibiaSession.editingCompetitionId
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(ManageCompetitionsFragment()) }
        if (!editingId.isNullOrBlank()) {
            apiInBackground({ parseCompetitionList(HabibiaApi.get("/competitions")) }) { competitions ->
                if (!isAdded) return@apiInBackground
                val existing = competitions.find { it.competitionId == editingId } ?: return@apiInBackground
                view.findViewById<TextInputEditText>(R.id.nameInput).setText(existing.competitionName)
                view.findViewById<TextInputEditText>(R.id.dateInput).setText(existing.competitionDate)
                view.findViewById<TextInputEditText>(R.id.deadlineInput).setText(existing.registrationDeadline)
                view.findViewById<TextInputEditText>(R.id.venueInput).setText(existing.venue)
                view.findViewById<TextInputEditText>(R.id.descriptionInput).setText(existing.description)
                view.findViewById<TextInputEditText>(R.id.statusInput).setText(existing.status)
            }
        }
        view.findViewById<Button>(R.id.saveCompetitionButton).setOnClickListener {
            val name = view.findViewById<TextInputEditText>(R.id.nameInput).text?.toString().orEmpty()
            if (name.isBlank()) {
                showMessage("Competition name is required")
                return@setOnClickListener
            }
            val body = JSONObject()
                .put("competitionName", name)
                .put("competitionDate", view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty())
                .put("registrationDeadline", view.findViewById<TextInputEditText>(R.id.deadlineInput).text?.toString().orEmpty())
                .put("venue", view.findViewById<TextInputEditText>(R.id.venueInput).text?.toString().orEmpty())
                .put("description", view.findViewById<TextInputEditText>(R.id.descriptionInput).text?.toString().orEmpty())
                .put("status", view.findViewById<TextInputEditText>(R.id.statusInput).text?.toString().orEmpty().ifBlank { "UPCOMING" })
                .toString()
            val saveButton = view.findViewById<Button>(R.id.saveCompetitionButton)
            saveButton.isEnabled = false
            apiInBackground(
                work = {
                    if (editingId.isNullOrBlank()) HabibiaApi.post("/competitions", body)
                    else HabibiaApi.put("/competitions/$editingId", body)
                },
                onError = { message ->
                    saveButton.isEnabled = true
                    showMessage(message)
                },
                onOk = {
                    HabibiaSession.pendingSnackbar = getString(R.string.competition_saved)
                    goTo(ManageCompetitionsFragment())
                }
            )
        }
    }
}

/** Admin announcement list. */
class ManageAnnouncementsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_announcements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumePendingMessage()
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(AdminDashboardFragment()) }
        view.findViewById<Button>(R.id.addAnnouncementButton).setOnClickListener {
            HabibiaSession.editingAnnouncementId = null
            goTo(AddEditAnnouncementFragment())
        }
        val container = view.findViewById<LinearLayout>(R.id.announcementsListContainer)
        var announcements = emptyList<Announcement>()
        fun render() {
            container.removeAllViews()
            announcements.forEach { announcement ->
                val item = layoutInflater.inflate(R.layout.item_manage_row, container, false)
                item.findViewById<TextView>(R.id.titleText).text = announcement.title
                item.findViewById<TextView>(R.id.subtitleText).text = formatDisplayDate(announcement.datePosted)
                item.findViewById<Button>(R.id.editButton).setOnClickListener {
                    HabibiaSession.editingAnnouncementId = announcement.announcementId
                    goTo(AddEditAnnouncementFragment())
                }
                item.findViewById<Button>(R.id.deleteButton).setOnClickListener {
                    confirmDelete {
                        apiInBackground({ HabibiaApi.delete("/announcements/${announcement.announcementId}") }) {
                            announcements = announcements.filter { it.announcementId != announcement.announcementId }
                            showMessage(getString(R.string.announcement_deleted))
                            render()
                        }
                    }
                }
                container.addView(item)
            }
        }
        apiInBackground({ parseAnnouncementList(HabibiaApi.get("/announcements")) }) { loaded ->
            if (!isAdded) return@apiInBackground
            announcements = loaded
            render()
        }
    }
}

/** Create or update an announcement; date is omitted when blank so the API sets it. */
class AddEditAnnouncementFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_edit_announcement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editingId = HabibiaSession.editingAnnouncementId
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(ManageAnnouncementsFragment()) }
        if (!editingId.isNullOrBlank()) {
            apiInBackground({ parseAnnouncementList(HabibiaApi.get("/announcements")) }) { announcements ->
                if (!isAdded) return@apiInBackground
                val existing = announcements.find { it.announcementId == editingId } ?: return@apiInBackground
                view.findViewById<TextInputEditText>(R.id.titleInput).setText(existing.title)
                view.findViewById<TextInputEditText>(R.id.contentInput).setText(existing.content)
                view.findViewById<TextInputEditText>(R.id.dateInput).setText(existing.datePosted)
            }
        }
        view.findViewById<Button>(R.id.saveAnnouncementButton).setOnClickListener {
            val title = view.findViewById<TextInputEditText>(R.id.titleInput).text?.toString().orEmpty()
            if (title.isBlank()) {
                showMessage("Title is required")
                return@setOnClickListener
            }
            val datePosted = view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty()
            val payload = JSONObject()
                .put("title", title)
                .put("content", view.findViewById<TextInputEditText>(R.id.contentInput).text?.toString().orEmpty())
            if (datePosted.isNotBlank()) payload.put("datePosted", datePosted)
            val body = payload.toString()
            val saveButton = view.findViewById<Button>(R.id.saveAnnouncementButton)
            saveButton.isEnabled = false
            apiInBackground(
                work = {
                    if (editingId.isNullOrBlank()) HabibiaApi.post("/announcements", body)
                    else HabibiaApi.put("/announcements/$editingId", body)
                },
                onError = { message ->
                    saveButton.isEnabled = true
                    showMessage(message)
                },
                onOk = {
                    HabibiaSession.pendingSnackbar = getString(R.string.announcement_saved)
                    goTo(ManageAnnouncementsFragment())
                }
            )
        }
    }
}

/** Admin beginner-resource list. */
class ManageResourcesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_resources, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumePendingMessage()
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(AdminDashboardFragment()) }
        view.findViewById<Button>(R.id.addResourceButton).setOnClickListener {
            HabibiaSession.editingResourceId = null
            goTo(AddEditResourceFragment())
        }
        val container = view.findViewById<LinearLayout>(R.id.resourcesListContainer)
        var resources = emptyList<BeginnerResource>()
        fun render(query: String) {
            container.removeAllViews()
            resources.filter {
                query.isBlank() || it.title.contains(query, true) || it.category.contains(query, true)
            }.forEach { resource ->
                val item = layoutInflater.inflate(R.layout.item_manage_row, container, false)
                item.findViewById<TextView>(R.id.titleText).text = resource.title
                item.findViewById<TextView>(R.id.subtitleText).text = resource.category
                item.findViewById<Button>(R.id.editButton).setOnClickListener {
                    HabibiaSession.editingResourceId = resource.resourceId
                    goTo(AddEditResourceFragment())
                }
                item.findViewById<Button>(R.id.deleteButton).setOnClickListener {
                    confirmDelete {
                        apiInBackground({ HabibiaApi.delete("/resources/${resource.resourceId}") }) {
                            resources = resources.filter { it.resourceId != resource.resourceId }
                            showMessage(getString(R.string.resource_deleted))
                            render(query)
                        }
                    }
                }
                container.addView(item)
            }
        }
        view.findViewById<TextInputEditText>(R.id.searchResourcesInput).addTextChangedListener(simpleWatcher { render(it) })
        apiInBackground({ parseResourceList(HabibiaApi.get("/resources")) }) { loaded ->
            if (!isAdded) return@apiInBackground
            resources = loaded
            render(view.findViewById<TextInputEditText>(R.id.searchResourcesInput).text?.toString().orEmpty())
        }
    }
}

/** Create or update a resource; blank category becomes Getting Started. */
class AddEditResourceFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_edit_resource, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val editingId = HabibiaSession.editingResourceId
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(ManageResourcesFragment()) }
        if (!editingId.isNullOrBlank()) {
            apiInBackground({ parseResourceList(HabibiaApi.get("/resources")) }) { resources ->
                if (!isAdded) return@apiInBackground
                val existing = resources.find { it.resourceId == editingId } ?: return@apiInBackground
                view.findViewById<TextInputEditText>(R.id.titleInput).setText(existing.title)
                view.findViewById<TextInputEditText>(R.id.categoryInput).setText(existing.category)
                view.findViewById<TextInputEditText>(R.id.descriptionInput).setText(existing.description)
                view.findViewById<TextInputEditText>(R.id.linkInput).setText(existing.resourceLink)
            }
        }
        view.findViewById<Button>(R.id.saveResourceButton).setOnClickListener {
            val title = view.findViewById<TextInputEditText>(R.id.titleInput).text?.toString().orEmpty()
            if (title.isBlank()) {
                showMessage("Title is required")
                return@setOnClickListener
            }
            val body = JSONObject()
                .put("title", title)
                .put("category", view.findViewById<TextInputEditText>(R.id.categoryInput).text?.toString().orEmpty().ifBlank { "Getting Started" })
                .put("description", view.findViewById<TextInputEditText>(R.id.descriptionInput).text?.toString().orEmpty())
                .put("resourceLink", view.findViewById<TextInputEditText>(R.id.linkInput).text?.toString().orEmpty())
                .toString()
            val saveButton = view.findViewById<Button>(R.id.saveResourceButton)
            saveButton.isEnabled = false
            apiInBackground(
                work = {
                    if (editingId.isNullOrBlank()) HabibiaApi.post("/resources", body)
                    else HabibiaApi.put("/resources/$editingId", body)
                },
                onError = { message ->
                    saveButton.isEnabled = true
                    showMessage(message)
                },
                onOk = {
                    HabibiaSession.pendingSnackbar = getString(R.string.resource_saved)
                    goTo(ManageResourcesFragment())
                }
            )
        }
    }
}

/** Club-wide counts from several list endpoints, not a dedicated stats API. */
class AdminStatisticsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(AdminDashboardFragment()) }
        apiInBackground({
            val members = parseClubMembers(HabibiaApi.get("/admin/members")).size
            val events = parseEventList(HabibiaApi.get("/events")).size
            val competitions = parseCompetitionList(HabibiaApi.get("/competitions")).size
            val resources = parseResourceList(HabibiaApi.get("/resources")).size
            val announcements = parseAnnouncementList(HabibiaApi.get("/announcements")).size
            listOf(members, events, competitions, resources, announcements)
        }) { counts ->
            if (!isAdded) return@apiInBackground
            view.findViewById<TextView>(R.id.membersStat).text = counts[0].toString()
            view.findViewById<TextView>(R.id.eventsStat).text = counts[1].toString()
            view.findViewById<TextView>(R.id.competitionsStat).text = counts[2].toString()
            view.findViewById<TextView>(R.id.resourcesStat).text = counts[3].toString()
            view.findViewById<TextView>(R.id.announcementsStat).text = counts[4].toString()
        }
    }
}

/**
 * Stores the Firebase ID token from `/auth/login` or confirm-verification,
 * copies the member into leftover dummy state for home tiles, and routes
 * by `role` (Admin vs Member).
 */
private fun Fragment.applyAuthSuccess(json: String) {
    val body = JSONObject(json)
    val memberJson = body.optJSONObject("member") ?: JSONObject()
    val member = parseClubMember(memberJson).let { parsed ->
        if (parsed.role.isBlank()) parsed.copy(role = body.optString("role")) else parsed
    }
    HabibiaSession.authToken = body.optString("token").ifBlank { null }
    HabibiaSession.loggedInMemberId = member.memberId.ifBlank { null }
    HabibiaSession.pendingEmail = null
    HabibiaSession.pendingPassword = null
    HabibiaDummyData.member = member
    when (body.optString("role")) {
        "Admin" -> openAdminApp()
        "Member" -> openMemberApp()
        else -> showMessage("Unknown role")
    }
}

private fun bindProfile(view: View, member: Member, profile: MemberProfile) {
    view.findViewById<TextView>(R.id.profileName).text = member.fullName
    view.findViewById<TextView>(R.id.memberSince).text = "Member since ${formatDisplayDate(member.dateJoined)}"
    view.findViewById<TextView>(R.id.experienceText).text = "Experience Level: ${profile.experienceLevel}"
    view.findViewById<TextView>(R.id.bowTypeText).text = "Bow Type: ${profile.bowType}"
    view.findViewById<TextView>(R.id.divisionText).text = "Division: ${profile.division}"
    view.findViewById<TextView>(R.id.emailText).text = "Email: ${member.email}"
    view.findViewById<TextView>(R.id.emergencyText).text = "Emergency Contact: ${profile.emergencyContact}"
}

private fun parseClubMember(obj: JSONObject): Member = Member(
    memberId = obj.optString("memberId"),
    firstName = obj.optString("firstName"),
    lastName = obj.optString("lastName"),
    email = obj.optString("email"),
    role = obj.optString("role"),
    emailVerified = obj.optBoolean("emailVerified"),
    dateJoined = obj.optString("dateJoined")
)

private fun parseClubMembers(json: String): List<Member> {
    val array = JSONObject(json).optJSONArray("members") ?: JSONArray()
    return (0 until array.length()).map { parseClubMember(array.getJSONObject(it)) }
}

private fun parseMemberProfile(obj: JSONObject): MemberProfile = MemberProfile(
    profileId = obj.optString("profileId"),
    memberId = obj.optString("memberId"),
    experienceLevel = obj.optString("experienceLevel"),
    bowType = obj.optString("bowType"),
    division = obj.optString("division"),
    emergencyContact = obj.optString("emergencyContact")
)

private fun parseEventList(json: String): List<ClubEvent> {
    val array = JSONObject(json).optJSONArray("events") ?: JSONArray()
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        ClubEvent(
            eventId = obj.optString("eventId"),
            title = obj.optString("title"),
            description = obj.optString("description"),
            eventDate = obj.optString("eventDate"),
            eventTime = obj.optString("eventTime"),
            location = obj.optString("location"),
            type = obj.optString("type").ifBlank { "Event" }
        )
    }
}

private fun parseCompetitionList(json: String): List<Competition> {
    val array = JSONObject(json).optJSONArray("competitions") ?: JSONArray()
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        Competition(
            competitionId = obj.optString("competitionId"),
            competitionName = obj.optString("competitionName"),
            competitionDate = obj.optString("competitionDate"),
            registrationDeadline = obj.optString("registrationDeadline"),
            venue = obj.optString("venue"),
            description = obj.optString("description"),
            status = obj.optString("status").ifBlank { "UPCOMING" }
        )
    }
}

private fun parseAnnouncementList(json: String): List<Announcement> {
    val array = JSONObject(json).optJSONArray("announcements") ?: JSONArray()
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        Announcement(
            announcementId = obj.optString("announcementId"),
            title = obj.optString("title"),
            content = obj.optString("content"),
            datePosted = obj.optString("datePosted")
        )
    }
}

private fun parseNotificationList(json: String): List<Notification> {
    val array = JSONObject(json).optJSONArray("notifications") ?: JSONArray()
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        Notification(
            notificationId = obj.optString("notificationId"),
            title = obj.optString("title"),
            message = obj.optString("message"),
            dateSent = obj.optString("dateSent"),
            isRead = obj.optBoolean("isRead")
        )
    }
}

private fun parseResourceList(json: String): List<BeginnerResource> {
    val array = JSONObject(json).optJSONArray("resources") ?: JSONArray()
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        BeginnerResource(
            resourceId = obj.optString("resourceId"),
            title = obj.optString("title"),
            category = obj.optString("category"),
            description = obj.optString("description"),
            resourceLink = obj.optString("resourceLink")
        )
    }
}

// ========================================
// END OF CODE
// ========================================


