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

class SplashFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }
}

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
            if (email.contains("admin", ignoreCase = true)) openAdminApp() else openMemberApp()
        }
        view.findViewById<View>(R.id.registerButton).setOnClickListener { goTo(RegisterFragment()) }
        view.findViewById<View>(R.id.demoMemberButton).setOnClickListener { openMemberApp() }
        view.findViewById<View>(R.id.demoAdminButton).setOnClickListener { openAdminApp() }
    }
}

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
            HabibiaDummyData.member = HabibiaDummyData.member.copy(firstName = first, lastName = last, email = email)
            goTo(EmailVerificationFragment())
        }
        view.findViewById<Button>(R.id.backToLoginButton).setOnClickListener { goTo(LoginFragment()) }
    }
}

class EmailVerificationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_email_verification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.verifyButton).setOnClickListener {
            showMessage("Email verified")
            openMemberApp()
        }
    }
}

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

class MemberDashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_member_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val member = HabibiaDummyData.member
        view.findViewById<TextView>(R.id.greetingText).text =
            "${HabibiaDummyData.greeting()}, ${member.firstName}"

        val unread = HabibiaDummyData.unreadCount()
        view.findViewById<View>(R.id.notificationDot).visibility =
            if (unread > 0) View.VISIBLE else View.GONE

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

class ClubCalendarFragment : Fragment() {
    private var filter = "All"
    private var query = ""

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
        render(view)
    }

    private fun render(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.eventsListContainer)
        container.removeAllViews()
        val eventItems = HabibiaDummyData.events.filter {
            val typeMatch = when (filter) {
                "All" -> true
                "Practice" -> it.type.equals("Practice", true)
                "Event" -> it.type.equals("Event", true)
                else -> false
            }
            typeMatch && (query.isBlank() || it.title.contains(query, true) || it.location.contains(query, true))
        }
        val competitionItems = if (filter == "All" || filter == "Competition") {
            HabibiaDummyData.competitions.filter {
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

class EventDetailsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_event_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val event = HabibiaDummyData.events.find { it.eventId == HabibiaSession.selectedEventId }
            ?: HabibiaDummyData.events.firstOrNull()
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navCalendar) }
        if (event == null) return
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

class CompetitionFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_competition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val container = view.findViewById<LinearLayout>(R.id.competitionsListContainer)
        fun render(query: String) {
            container.removeAllViews()
            val items = HabibiaDummyData.competitions.filter {
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
        render("")
    }
}

class CompetitionDetailsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_competition_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val competition = HabibiaDummyData.competitions.find { it.competitionId == HabibiaSession.selectedCompetitionId }
            ?: HabibiaDummyData.competitions.firstOrNull() ?: return
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            if (HabibiaSession.isAdmin) goTo(ManageCompetitionsFragment()) else goTo(CompetitionFragment())
        }
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

class AnnouncementFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_announcement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp() }
        val container = view.findViewById<LinearLayout>(R.id.announcementsListContainer)
        container.removeAllViews()
        HabibiaDummyData.announcements.forEach { announcement ->
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

class NotificationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp() }
        val container = view.findViewById<LinearLayout>(R.id.notificationsListContainer)
        fun render() {
            container.removeAllViews()
            if (HabibiaDummyData.notifications.isEmpty()) {
                container.bindEmptyState(
                    getString(R.string.empty_notifications_title),
                    getString(R.string.empty_notifications_body)
                )
                return
            }
            HabibiaDummyData.notifications.forEach { notification ->
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
                    notification.isRead = true
                    render()
                }
                container.addView(item)
            }
        }
        render()
    }
}

class BeginnerResourcesFragment : Fragment() {
    private var category = "All"
    private var query = ""

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
        render(view)
    }

    private fun render(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.resourcesListContainer)
        container.removeAllViews()
        val items = HabibiaDummyData.resources.filter {
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

class ResourceDetailsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_resource_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val resource = HabibiaDummyData.resources.find { it.resourceId == HabibiaSession.selectedResourceId }
            ?: HabibiaDummyData.resources.firstOrNull() ?: return
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { openMemberApp(R.id.navResources) }
        view.findViewById<TextView>(R.id.resourceCategory).text = resource.category
        view.findViewById<TextView>(R.id.resourceTitle).text = resource.title
        view.findViewById<TextView>(R.id.resourceDescription).text = resource.description
        view.findViewById<Button>(R.id.openLinkButton).setOnClickListener {
            showMessage("Opening: ${resource.resourceLink}")
        }
    }
}

class ProfileFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumePendingMessage()
        val member = HabibiaDummyData.member
        val profile = HabibiaDummyData.profile
        view.findViewById<TextView>(R.id.profileName).text = member.fullName
        view.findViewById<TextView>(R.id.memberSince).text = "Member since ${formatDisplayDate(member.dateJoined)}"
        view.findViewById<TextView>(R.id.experienceText).text = "Experience Level: ${profile.experienceLevel}"
        view.findViewById<TextView>(R.id.bowTypeText).text = "Bow Type: ${profile.bowType}"
        view.findViewById<TextView>(R.id.divisionText).text = "Division: ${profile.division}"
        view.findViewById<TextView>(R.id.emailText).text = "Email: ${member.email}"
        view.findViewById<TextView>(R.id.emergencyText).text = "Emergency Contact: ${profile.emergencyContact}"
        view.findViewById<Button>(R.id.editProfileButton).setOnClickListener { goTo(EditProfileFragment()) }
        view.findViewById<Button>(R.id.settingsButton).setOnClickListener {
            showMessage("Settings are available in a future release")
        }
        view.findViewById<Button>(R.id.logoutButton).setOnClickListener { goTo(LoginFragment()) }
    }
}

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
            member.firstName = first
            member.lastName = last
            member.email = view.findViewById<TextInputEditText>(R.id.emailInput).text?.toString().orEmpty()
            profile.experienceLevel = view.findViewById<TextInputEditText>(R.id.experienceInput).text?.toString().orEmpty()
            profile.bowType = view.findViewById<TextInputEditText>(R.id.bowTypeInput).text?.toString().orEmpty()
            profile.division = view.findViewById<TextInputEditText>(R.id.divisionInput).text?.toString().orEmpty()
            profile.emergencyContact = view.findViewById<TextInputEditText>(R.id.emergencyInput).text?.toString().orEmpty()
            HabibiaSession.pendingSnackbar = getString(R.string.profile_updated)
            openMemberApp(R.id.navProfile)
        }
    }
}

class AdminDashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        HabibiaSession.isAdmin = true
        consumePendingMessage()
        view.findViewById<TextView>(R.id.totalMembersText).text = HabibiaDummyData.members.size.toString()
        view.findViewById<TextView>(R.id.upcomingEventsText).text = HabibiaDummyData.events.size.toString()
        view.findViewById<TextView>(R.id.upcomingCompetitionsText).text = HabibiaDummyData.competitions.size.toString()
        view.findViewById<TextView>(R.id.scoresRecordedText).text = HabibiaDummyData.scoreSessions.size.toString()
        view.findViewById<Button>(R.id.memberProgressButton).setOnClickListener { goTo(AdminMemberListFragment()) }
        view.findViewById<Button>(R.id.manageMembersButton).setOnClickListener { goTo(ManageMembersFragment()) }
        view.findViewById<Button>(R.id.manageEventsButton).setOnClickListener { goTo(ManageEventsFragment()) }
        view.findViewById<Button>(R.id.manageCompetitionsButton).setOnClickListener { goTo(ManageCompetitionsFragment()) }
        view.findViewById<Button>(R.id.manageAnnouncementsButton).setOnClickListener { goTo(ManageAnnouncementsFragment()) }
        view.findViewById<Button>(R.id.manageResourcesButton).setOnClickListener { goTo(ManageResourcesFragment()) }
        view.findViewById<Button>(R.id.statisticsButton).setOnClickListener { goTo(AdminStatisticsFragment()) }
        view.findViewById<Button>(R.id.logoutAdminButton).setOnClickListener {
            HabibiaSession.isAdmin = false
            HabibiaSession.selectedMemberId = null
            goTo(LoginFragment())
        }
    }
}

class ManageMembersFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_manage_members, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(AdminDashboardFragment()) }
        val container = view.findViewById<LinearLayout>(R.id.membersListContainer)
        fun render(query: String) {
            container.removeAllViews()
            HabibiaDummyData.members.filter {
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
                        HabibiaDummyData.members.remove(member)
                        HabibiaSession.pendingSnackbar = "Member deleted"
                        render(query)
                        showMessage("Member deleted")
                    }
                }
                container.addView(item)
            }
        }
        view.findViewById<TextInputEditText>(R.id.searchMembersInput).addTextChangedListener(simpleWatcher { render(it) })
        render("")
    }
}

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
        fun render(query: String) {
            container.removeAllViews()
            HabibiaDummyData.events.filter {
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
                        HabibiaDummyData.events.remove(event)
                        showMessage(getString(R.string.event_deleted))
                        render(query)
                    }
                }
                container.addView(item)
            }
        }
        view.findViewById<TextInputEditText>(R.id.searchEventsInput).addTextChangedListener(simpleWatcher { render(it) })
        render("")
    }
}

class AddEditEventFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_edit_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val existing = HabibiaDummyData.events.find { it.eventId == HabibiaSession.editingEventId }
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(ManageEventsFragment()) }
        if (existing != null) {
            view.findViewById<TextInputEditText>(R.id.titleInput).setText(existing.title)
            view.findViewById<TextInputEditText>(R.id.dateInput).setText(existing.eventDate)
            view.findViewById<TextInputEditText>(R.id.timeInput).setText(existing.eventTime)
            view.findViewById<TextInputEditText>(R.id.locationInput).setText(existing.location)
            view.findViewById<TextInputEditText>(R.id.descriptionInput).setText(existing.description)
            view.findViewById<TextInputEditText>(R.id.typeInput).setText(existing.type)
        }
        view.findViewById<Button>(R.id.saveEventButton).setOnClickListener {
            val title = view.findViewById<TextInputEditText>(R.id.titleInput).text?.toString().orEmpty()
            if (title.isBlank()) {
                showMessage("Title is required")
                return@setOnClickListener
            }
            if (existing != null) {
                existing.title = title
                existing.eventDate = view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty()
                existing.eventTime = view.findViewById<TextInputEditText>(R.id.timeInput).text?.toString().orEmpty()
                existing.location = view.findViewById<TextInputEditText>(R.id.locationInput).text?.toString().orEmpty()
                existing.description = view.findViewById<TextInputEditText>(R.id.descriptionInput).text?.toString().orEmpty()
                existing.type = view.findViewById<TextInputEditText>(R.id.typeInput).text?.toString().orEmpty().ifBlank { "Event" }
            } else {
                HabibiaDummyData.events.add(
                    ClubEvent(
                        eventId = "E${System.currentTimeMillis()}",
                        title = title,
                        description = view.findViewById<TextInputEditText>(R.id.descriptionInput).text?.toString().orEmpty(),
                        eventDate = view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty(),
                        eventTime = view.findViewById<TextInputEditText>(R.id.timeInput).text?.toString().orEmpty(),
                        location = view.findViewById<TextInputEditText>(R.id.locationInput).text?.toString().orEmpty(),
                        type = view.findViewById<TextInputEditText>(R.id.typeInput).text?.toString().orEmpty().ifBlank { "Event" }
                    )
                )
            }
            HabibiaSession.pendingSnackbar = getString(R.string.event_saved)
            goTo(ManageEventsFragment())
        }
    }
}

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
        fun render(query: String) {
            container.removeAllViews()
            HabibiaDummyData.competitions.filter {
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
                        HabibiaDummyData.competitions.remove(competition)
                        showMessage(getString(R.string.competition_deleted))
                        render(query)
                    }
                }
                container.addView(item)
            }
        }
        view.findViewById<TextInputEditText>(R.id.searchCompetitionsInput).addTextChangedListener(simpleWatcher { render(it) })
        render("")
    }
}

class AddEditCompetitionFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_edit_competition, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val existing = HabibiaDummyData.competitions.find { it.competitionId == HabibiaSession.editingCompetitionId }
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(ManageCompetitionsFragment()) }
        if (existing != null) {
            view.findViewById<TextInputEditText>(R.id.nameInput).setText(existing.competitionName)
            view.findViewById<TextInputEditText>(R.id.dateInput).setText(existing.competitionDate)
            view.findViewById<TextInputEditText>(R.id.deadlineInput).setText(existing.registrationDeadline)
            view.findViewById<TextInputEditText>(R.id.venueInput).setText(existing.venue)
            view.findViewById<TextInputEditText>(R.id.descriptionInput).setText(existing.description)
            view.findViewById<TextInputEditText>(R.id.statusInput).setText(existing.status)
        }
        view.findViewById<Button>(R.id.saveCompetitionButton).setOnClickListener {
            val name = view.findViewById<TextInputEditText>(R.id.nameInput).text?.toString().orEmpty()
            if (name.isBlank()) {
                showMessage("Competition name is required")
                return@setOnClickListener
            }
            if (existing != null) {
                existing.competitionName = name
                existing.competitionDate = view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty()
                existing.registrationDeadline = view.findViewById<TextInputEditText>(R.id.deadlineInput).text?.toString().orEmpty()
                existing.venue = view.findViewById<TextInputEditText>(R.id.venueInput).text?.toString().orEmpty()
                existing.description = view.findViewById<TextInputEditText>(R.id.descriptionInput).text?.toString().orEmpty()
                existing.status = view.findViewById<TextInputEditText>(R.id.statusInput).text?.toString().orEmpty().ifBlank { "UPCOMING" }
            } else {
                HabibiaDummyData.competitions.add(
                    Competition(
                        competitionId = "C${System.currentTimeMillis()}",
                        competitionName = name,
                        competitionDate = view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty(),
                        registrationDeadline = view.findViewById<TextInputEditText>(R.id.deadlineInput).text?.toString().orEmpty(),
                        venue = view.findViewById<TextInputEditText>(R.id.venueInput).text?.toString().orEmpty(),
                        description = view.findViewById<TextInputEditText>(R.id.descriptionInput).text?.toString().orEmpty(),
                        status = view.findViewById<TextInputEditText>(R.id.statusInput).text?.toString().orEmpty().ifBlank { "UPCOMING" }
                    )
                )
            }
            HabibiaSession.pendingSnackbar = getString(R.string.competition_saved)
            goTo(ManageCompetitionsFragment())
        }
    }
}

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
        fun render() {
            container.removeAllViews()
            HabibiaDummyData.announcements.forEach { announcement ->
                val item = layoutInflater.inflate(R.layout.item_manage_row, container, false)
                item.findViewById<TextView>(R.id.titleText).text = announcement.title
                item.findViewById<TextView>(R.id.subtitleText).text = formatDisplayDate(announcement.datePosted)
                item.findViewById<Button>(R.id.editButton).setOnClickListener {
                    HabibiaSession.editingAnnouncementId = announcement.announcementId
                    goTo(AddEditAnnouncementFragment())
                }
                item.findViewById<Button>(R.id.deleteButton).setOnClickListener {
                    confirmDelete {
                        HabibiaDummyData.announcements.remove(announcement)
                        showMessage(getString(R.string.announcement_deleted))
                        render()
                    }
                }
                container.addView(item)
            }
        }
        render()
    }
}

class AddEditAnnouncementFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_edit_announcement, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val existing = HabibiaDummyData.announcements.find { it.announcementId == HabibiaSession.editingAnnouncementId }
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(ManageAnnouncementsFragment()) }
        if (existing != null) {
            view.findViewById<TextInputEditText>(R.id.titleInput).setText(existing.title)
            view.findViewById<TextInputEditText>(R.id.contentInput).setText(existing.content)
            view.findViewById<TextInputEditText>(R.id.dateInput).setText(existing.datePosted)
        }
        view.findViewById<Button>(R.id.saveAnnouncementButton).setOnClickListener {
            val title = view.findViewById<TextInputEditText>(R.id.titleInput).text?.toString().orEmpty()
            if (title.isBlank()) {
                showMessage("Title is required")
                return@setOnClickListener
            }
            if (existing != null) {
                existing.title = title
                existing.content = view.findViewById<TextInputEditText>(R.id.contentInput).text?.toString().orEmpty()
                existing.datePosted = view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty()
            } else {
                HabibiaDummyData.announcements.add(
                    Announcement(
                        announcementId = "A${System.currentTimeMillis()}",
                        title = title,
                        content = view.findViewById<TextInputEditText>(R.id.contentInput).text?.toString().orEmpty(),
                        datePosted = view.findViewById<TextInputEditText>(R.id.dateInput).text?.toString().orEmpty().ifBlank { "2026-09-12" }
                    )
                )
            }
            HabibiaSession.pendingSnackbar = getString(R.string.announcement_saved)
            goTo(ManageAnnouncementsFragment())
        }
    }
}

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
        fun render(query: String) {
            container.removeAllViews()
            HabibiaDummyData.resources.filter {
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
                        HabibiaDummyData.resources.remove(resource)
                        showMessage(getString(R.string.resource_deleted))
                        render(query)
                    }
                }
                container.addView(item)
            }
        }
        view.findViewById<TextInputEditText>(R.id.searchResourcesInput).addTextChangedListener(simpleWatcher { render(it) })
        render("")
    }
}

class AddEditResourceFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_edit_resource, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val existing = HabibiaDummyData.resources.find { it.resourceId == HabibiaSession.editingResourceId }
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(ManageResourcesFragment()) }
        if (existing != null) {
            view.findViewById<TextInputEditText>(R.id.titleInput).setText(existing.title)
            view.findViewById<TextInputEditText>(R.id.categoryInput).setText(existing.category)
            view.findViewById<TextInputEditText>(R.id.descriptionInput).setText(existing.description)
            view.findViewById<TextInputEditText>(R.id.linkInput).setText(existing.resourceLink)
        }
        view.findViewById<Button>(R.id.saveResourceButton).setOnClickListener {
            val title = view.findViewById<TextInputEditText>(R.id.titleInput).text?.toString().orEmpty()
            if (title.isBlank()) {
                showMessage("Title is required")
                return@setOnClickListener
            }
            if (existing != null) {
                existing.title = title
                existing.category = view.findViewById<TextInputEditText>(R.id.categoryInput).text?.toString().orEmpty()
                existing.description = view.findViewById<TextInputEditText>(R.id.descriptionInput).text?.toString().orEmpty()
                existing.resourceLink = view.findViewById<TextInputEditText>(R.id.linkInput).text?.toString().orEmpty()
            } else {
                HabibiaDummyData.resources.add(
                    BeginnerResource(
                        resourceId = "R${System.currentTimeMillis()}",
                        title = title,
                        category = view.findViewById<TextInputEditText>(R.id.categoryInput).text?.toString().orEmpty().ifBlank { "Getting Started" },
                        description = view.findViewById<TextInputEditText>(R.id.descriptionInput).text?.toString().orEmpty(),
                        resourceLink = view.findViewById<TextInputEditText>(R.id.linkInput).text?.toString().orEmpty()
                    )
                )
            }
            HabibiaSession.pendingSnackbar = getString(R.string.resource_saved)
            goTo(ManageResourcesFragment())
        }
    }
}

class AdminStatisticsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ImageButton>(R.id.backButton).setOnClickListener { goTo(AdminDashboardFragment()) }
        view.findViewById<TextView>(R.id.membersStat).text = HabibiaDummyData.members.size.toString()
        view.findViewById<TextView>(R.id.eventsStat).text = HabibiaDummyData.events.size.toString()
        view.findViewById<TextView>(R.id.competitionsStat).text = HabibiaDummyData.competitions.size.toString()
        view.findViewById<TextView>(R.id.scoresStat).text = HabibiaDummyData.scoreSessions.size.toString()
        view.findViewById<TextView>(R.id.resourcesStat).text = HabibiaDummyData.resources.size.toString()
        view.findViewById<TextView>(R.id.announcementsStat).text = HabibiaDummyData.announcements.size.toString()
    }
}

