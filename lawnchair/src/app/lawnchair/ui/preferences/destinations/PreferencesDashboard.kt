/*
 * Copyright 2021, Lawnchair
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.ui.preferences.destinations

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import app.lawnchair.LawnchairApp
import app.lawnchair.LawnchairLauncher
import app.lawnchair.preferences.getAdapter
import app.lawnchair.preferences.observeAsState
import app.lawnchair.preferences.preferenceManager
import app.lawnchair.preferences2.firstCached
import app.lawnchair.preferences2.preferenceManager2
import app.lawnchair.ui.OverflowMenuGrouped
import app.lawnchair.ui.preferences.components.AnnouncementPreference
import app.lawnchair.ui.preferences.components.controls.WarningPreference
import app.lawnchair.ui.preferences.components.layout.ClickableIcon
import app.lawnchair.ui.preferences.components.layout.ExpandAndShrink
import app.lawnchair.ui.preferences.components.layout.PreferenceLayout
import app.lawnchair.ui.preferences.components.layout.PreferenceTemplate
import app.lawnchair.ui.preferences.components.layout.ProvideDescriptionTextStyle
import app.lawnchair.ui.preferences.data.liveinfo.SyncLiveInformation
import app.lawnchair.ui.preferences.navigation.About
import app.lawnchair.ui.preferences.navigation.AppDrawer
import app.lawnchair.ui.preferences.navigation.BackupAndRestore
import app.lawnchair.ui.preferences.navigation.DebugMenu
import app.lawnchair.ui.preferences.navigation.Dock
import app.lawnchair.ui.preferences.navigation.ExperimentalFeatures
import app.lawnchair.ui.preferences.navigation.Folders
import app.lawnchair.ui.preferences.navigation.General
import app.lawnchair.ui.preferences.navigation.Gestures
import app.lawnchair.ui.preferences.navigation.HomeScreen
import app.lawnchair.ui.preferences.navigation.PreferenceRootRoute
import app.lawnchair.ui.preferences.navigation.Quickstep
import app.lawnchair.ui.preferences.navigation.Search
import app.lawnchair.ui.preferences.navigation.Smartspace
import app.lawnchair.ui.util.addIf
import app.lawnchair.util.isDefaultLauncher
import app.lawnchair.util.restartLauncher
import com.android.launcher3.BuildConfig
import com.android.launcher3.R
import com.android.launcher3.util.MSDLPlayerWrapper
import com.google.android.msdl.data.model.MSDLToken

/** Dados de uma linha da tela de configurações. */
private class DashItem(
    val label: String,
    val description: String,
    val iconRes: Int,
    val route: PreferenceRootRoute,
    val selected: Boolean,
    val visible: Boolean = true,
)

/** Pares (fundo do círculo, cor do ícone) em tons pastel. */
private val circlePalette = listOf(
    0xFF8FD0F2L to 0xFF0B3A52L,
    0xFFA9C7FFL to 0xFF0C2D6BL,
    0xFFB8E6C4L to 0xFF0F4A25L,
    0xFFF5C2E7L to 0xFF5A1B46L,
    0xFFFFD9A8L to 0xFF6A3B00L,
    0xFFC9B8FFL to 0xFF2B1A6BL,
)

/** Cantos grandes nas pontas do grupo e pequenos no meio. */
private fun groupShape(index: Int, count: Int): RoundedCornerShape {
    val big = 24.dp
    val small = 6.dp
    return when {
        index < 0 -> RoundedCornerShape(small)
        count <= 1 -> RoundedCornerShape(big)
        index == 0 -> RoundedCornerShape(topStart = big, topEnd = big, bottomStart = small, bottomEnd = small)
        index == count - 1 -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = big, bottomEnd = big)
        else -> RoundedCornerShape(small)
    }
}

@Composable
private fun ExpressiveGroup(
    items: List<DashItem>,
    colorOffset: Int,
    onNavigate: (PreferenceRootRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleItems = items.filter { it.visible }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        items.forEachIndexed { i, item ->
            ExpandAndShrink(visible = item.visible) {
                val shape = groupShape(visibleItems.indexOf(item), visibleItems.size)
                val colors = circlePalette[(colorOffset + i) % circlePalette.size]
                val container = if (item.selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                }
                Row(
                    modifier = Modifier
                        .padding(bottom = 2.dp)
                        .fillMaxWidth()
                        .clip(shape)
                        .background(container)
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(colors.first)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = null,
                            tint = Color(colors.second),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreferencesDashboard(
    currentRoute: PreferenceRootRoute,
    onNavigate: (PreferenceRootRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    SyncLiveInformation()
    val prefs = preferenceManager()
    val prefs2 = preferenceManager2()

    val aboutDescrption = if (prefs.hideVersionInfo.get()) {
        prefs.pseudonymVersion.get()
    } else {
        "${context.getString(R.string.derived_app_name)} ${BuildConfig.MAJOR_VERSION}"
    }

    PreferenceLayout(
        label = stringResource(id = R.string.settings),
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        backArrowVisible = false,
        actions = { PreferencesOverflowMenu(currentRoute = currentRoute, onNavigate = onNavigate) },
    ) {
        AnnouncementPreference()

        if (BuildConfig.APPLICATION_ID.contains("nightly") || BuildConfig.DEBUG) {
            PreferencesDebugWarning()
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!context.isDefaultLauncher()) {
            PreferencesSetDefaultLauncherWarning()
            Spacer(modifier = Modifier.height(8.dp))
        }

        val deckLayout = prefs2.deckLayout.getAdapter()
        val isSmartspaceEnabled = prefs2.enableSmartspace.firstCached()

        val groupMain = listOf(
            DashItem(
                label = stringResource(R.string.general_label),
                description = stringResource(R.string.general_description),
                iconRes = R.drawable.ic_general,
                route = General,
                selected = currentRoute is General,
            ),
            DashItem(
                label = stringResource(R.string.home_screen_label),
                description = stringResource(R.string.home_screen_description),
                iconRes = R.drawable.ic_home_screen,
                route = HomeScreen,
                selected = currentRoute is HomeScreen,
            ),
            DashItem(
                label = stringResource(id = R.string.smartspace_widget),
                description = stringResource(R.string.smartspace_widget_description),
                iconRes = if (isSmartspaceEnabled) R.drawable.ic_smartspace else R.drawable.ic_smartspace_off,
                route = Smartspace,
                selected = currentRoute is Smartspace,
            ),
        )

        val groupLayout = listOf(
            DashItem(
                label = stringResource(R.string.dock_label),
                description = stringResource(R.string.dock_description),
                iconRes = R.drawable.ic_dock,
                route = Dock,
                selected = currentRoute is Dock,
            ),
            DashItem(
                label = stringResource(R.string.app_drawer_label),
                description = stringResource(R.string.app_drawer_description),
                iconRes = R.drawable.ic_apps,
                route = AppDrawer,
                selected = currentRoute is AppDrawer,
                visible = !deckLayout.state.value,
            ),
            DashItem(
                label = stringResource(R.string.search_bar_label),
                description = stringResource(R.string.drawer_search_description),
                iconRes = R.drawable.ic_search,
                route = Search(),
                selected = currentRoute is Search,
            ),
            DashItem(
                label = stringResource(R.string.folders_label),
                description = stringResource(R.string.folders_description),
                iconRes = R.drawable.ic_folder,
                route = Folders,
                selected = currentRoute is Folders,
            ),
        )

        val groupBehavior = listOf(
            DashItem(
                label = stringResource(id = R.string.gestures_label),
                description = stringResource(R.string.gestures_description),
                iconRes = R.drawable.ic_gestures,
                route = Gestures,
                selected = currentRoute is Gestures,
            ),
            DashItem(
                label = stringResource(id = R.string.quickstep_label),
                description = stringResource(id = R.string.quickstep_description),
                iconRes = R.drawable.ic_quickstep,
                route = Quickstep,
                selected = currentRoute is Quickstep,
                visible = LawnchairApp.isRecentsEnabled || BuildConfig.DEBUG,
            ),
        )

        val groupOther = listOf(
            DashItem(
                label = stringResource(R.string.backup_and_restore_label),
                description = stringResource(R.string.backup_and_restore_description),
                iconRes = R.drawable.backup_restore,
                route = BackupAndRestore,
                selected = currentRoute is BackupAndRestore,
            ),
            DashItem(
                label = stringResource(R.string.about_label),
                description = aboutDescrption,
                iconRes = R.drawable.ic_about,
                route = About,
                selected = currentRoute is About,
            ),
        )

        ExpressiveGroup(items = groupMain, colorOffset = 0, onNavigate = onNavigate)
        Spacer(modifier = Modifier.height(12.dp))
        ExpressiveGroup(items = groupLayout, colorOffset = 3, onNavigate = onNavigate)
        Spacer(modifier = Modifier.height(12.dp))
        ExpressiveGroup(items = groupBehavior, colorOffset = 1, onNavigate = onNavigate)
        Spacer(modifier = Modifier.height(12.dp))
        ExpressiveGroup(items = groupOther, colorOffset = 2, onNavigate = onNavigate)
    }
}

@Composable
fun RowScope.PreferencesOverflowMenu(
    currentRoute: PreferenceRootRoute,
    onNavigate: (PreferenceRootRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enableDebug by preferenceManager().enableDebugMenu.observeAsState()
    val highlightColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    val highlightShape = MaterialTheme.shapes.large

    if (enableDebug) {
        ClickableIcon(
            imageVector = Icons.Rounded.Build,
            onClick = { onNavigate(DebugMenu) },
            modifier = Modifier.addIf(currentRoute == DebugMenu) {
                Modifier
                    .clip(highlightShape)
                    .background(highlightColor)
            },
        )
    }
    val context = LocalContext.current

    OverflowMenuGrouped(
        modifier = modifier.addIf(
            listOf(ExperimentalFeatures).any {
                currentRoute == it
            },
        ) {
            Modifier
                .clip(highlightShape)
                .background(highlightColor)
        },
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(0, 1),
        ) {
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_about),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = {
                    openAppInfo(context)
                    hideMenu()
                },
                text = {
                    Text(text = stringResource(id = R.string.app_info_drop_target_label))
                },
            )
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = {
                    restartLauncher(context)
                    hideMenu()
                },
                text = {
                    Text(text = stringResource(id = R.string.debug_restart_launcher))
                },
            )
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                onClick = {
                    onNavigate(ExperimentalFeatures)
                    hideMenu()
                },
                text = {
                    Text(text = stringResource(id = R.string.experimental_features_label))
                },
            )
        }

        Spacer(Modifier.height(MenuDefaults.GroupSpacing))
    }
}

@Composable
fun PreferencesDebugWarning(
    modifier: Modifier = Modifier,
) {
    WarningPreference(
        // Don't move to strings.xml, no need to translate this warning
        text = "You are using a development build, which may contain bugs and broken features. Use at your own risk!",
        modifier = modifier.padding(horizontal = 16.dp),
        standalone = true,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    )
}

@Composable
fun PreferencesSetDefaultLauncherWarning(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mMSDLPlayerWrapper = MSDLPlayerWrapper.INSTANCE.get(context)
    Surface(
        modifier = modifier.padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        PreferenceTemplate(
            modifier = Modifier,
            onClick = {
                mMSDLPlayerWrapper.playToken(MSDLToken.TAP_MEDIUM_EMPHASIS)
                Intent(Settings.ACTION_HOME_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .let { context.startActivity(it) }
                (context as? Activity)?.finish()
            },
            title = {
                ProvideDescriptionTextStyle {
                    Text(
                        text = stringResource(id = R.string.set_default_launcher_tip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            startWidget = {
                Icon(
                    imageVector = Icons.Rounded.TipsAndUpdates,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = null,
                )
            },
        )
    }
}

fun openAppInfo(context: Context) {
    val launcherApps = context.getSystemService<LauncherApps>()
    val componentName = ComponentName(context, LawnchairLauncher::class.java)
    launcherApps?.startAppDetailsActivity(componentName, Process.myUserHandle(), null, null)
}
