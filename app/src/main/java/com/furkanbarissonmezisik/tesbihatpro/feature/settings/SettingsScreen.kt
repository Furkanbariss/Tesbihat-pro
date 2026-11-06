package com.furkanbarissonmezisik.tesbihatpro.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.furkanbarissonmezisik.tesbihatpro.BuildConfig
import com.furkanbarissonmezisik.tesbihatpro.R
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.AppSettings
import com.furkanbarissonmezisik.tesbihatpro.core.domain.model.ThemeMode
import com.furkanbarissonmezisik.tesbihatpro.core.ui.BeadStyles
import com.furkanbarissonmezisik.tesbihatpro.core.ui.components.BannerAd
import com.furkanbarissonmezisik.tesbihatpro.core.ui.components.DetailTopBar
import com.furkanbarissonmezisik.tesbihatpro.core.ui.components.SectionTitle
import com.furkanbarissonmezisik.tesbihatpro.core.ui.components.findActivity

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val settings by viewModel.settings.collectAsState()
    val privacyOptionsRequired by viewModel.privacyOptionsRequired.collectAsState()
    val view = LocalView.current
    SettingsScreen(
        settings = settings,
        versionName = BuildConfig.VERSION_NAME,
        privacyOptionsRequired = privacyOptionsRequired,
        onBack = onBack,
        onSoundChange = viewModel::onSoundChange,
        onHapticsChange = viewModel::onHapticsChange,
        onVolumeKeysChange = viewModel::onVolumeKeysChange,
        onKeepScreenOnChange = viewModel::onKeepScreenOnChange,
        onThemeChange = viewModel::onThemeChange,
        onBeadStyleChange = viewModel::onBeadStyleChange,
        onPrivacyOptions = { viewModel.onPrivacyOptions(view.context.findActivity()) },
        adBanner = { BannerAd(viewModel.adsController) },
    )
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    versionName: String,
    privacyOptionsRequired: Boolean,
    onBack: () -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onVolumeKeysChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onBeadStyleChange: (String) -> Unit,
    onPrivacyOptions: () -> Unit,
    adBanner: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            DetailTopBar(title = stringResource(R.string.screen_settings), onBack = onBack)
        },
        bottomBar = adBanner,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item { SectionTitle(stringResource(R.string.settings_section_feedback)) }
            item {
                SwitchRow(
                    title = stringResource(R.string.settings_sound),
                    checked = settings.soundEnabled,
                    onChange = onSoundChange,
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.settings_vibration),
                    checked = settings.hapticsEnabled,
                    onChange = onHapticsChange,
                )
            }
            item { SectionTitle(stringResource(R.string.settings_section_counting)) }
            item {
                SwitchRow(
                    title = stringResource(R.string.settings_volume_keys),
                    hint = stringResource(R.string.settings_volume_keys_hint),
                    checked = settings.volumeKeysEnabled,
                    onChange = onVolumeKeysChange,
                )
            }
            item {
                SwitchRow(
                    title = stringResource(R.string.settings_keep_screen_on),
                    hint = stringResource(R.string.settings_keep_screen_on_hint),
                    checked = settings.keepScreenOn,
                    onChange = onKeepScreenOnChange,
                )
            }
            item { SectionTitle(stringResource(R.string.settings_section_appearance)) }
            item {
                ChoiceRow(
                    title = stringResource(R.string.settings_theme),
                    options = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                        ThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                    ),
                    selected = settings.theme,
                    onSelect = onThemeChange,
                )
            }
            item {
                ChoiceRow(
                    title = stringResource(R.string.settings_bead_style),
                    options = BeadStyles.all.map { it.id to stringResource(it.nameRes) },
                    selected = BeadStyles.find(settings.beadStyleId).id,
                    onSelect = onBeadStyleChange,
                )
            }
            item { SectionTitle(stringResource(R.string.settings_section_about)) }
            if (privacyOptionsRequired) {
                item {
                    ListItem(
                        modifier = Modifier.clickable(onClick = onPrivacyOptions),
                        headlineContent = {
                            Text(stringResource(R.string.settings_privacy_options))
                        },
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = {
                        Text(stringResource(R.string.settings_about_version, versionName))
                    },
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    hint: String? = null,
) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        headlineContent = { Text(title) },
        supportingContent = hint?.let { text -> { Text(text) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) {
                    Text(label)
                }
            }
        }
    }
}
