package com.opensource.autonomous.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandEmerald
import com.example.ui.theme.ProfitGreen
import com.opensource.autonomous.agent.data.SettingsEntity
import com.opensource.autonomous.agent.ui.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currentSettings by viewModel.settings.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val generatedMarketing by viewModel.generatedMarketing.collectAsState()

    var allowedStyles by remember(currentSettings) { mutableStateOf(currentSettings?.allowedStyles ?: "Minimalist line art, Vintage botanical, Retro typographic 70s, Cyber-grid geometric, Cottagecore watercolor") }
    var forbiddenItems by remember(currentSettings) { mutableStateOf(currentSettings?.forbiddenItems ?: "Trademarked logos, Disney/Marvel characters, celebrity names, copyrighted song lyrics, brand slogans") }
    var preferredPalettes by remember(currentSettings) { mutableStateOf(currentSettings?.preferredPalettes ?: "Earthy terracotta, Charcoal slate, Pastel sage, Amber gold, Monochrome ink") }
    var ollamaUrl by remember(currentSettings) { mutableStateOf(currentSettings?.ollamaEndpoint ?: "http://10.0.2.2:11434") }
    var ollamaModel by remember(currentSettings) { mutableStateOf(currentSettings?.ollamaModel ?: "llama3") }

    var marketingNicheInput by remember { mutableStateOf("Cozy Coffee & Coding Merch") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Settings & Autonomous Team",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Written design specifications, self-hosted open-weight LLM endpoint, and organic marketing studio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Written Design Specifications Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("design_specs_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brush, contentDescription = null, tint = BrandEmerald)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Written Design Specifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = allowedStyles,
                        onValueChange = { allowedStyles = it },
                        label = { Text("Allowed Aesthetic Styles") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = forbiddenItems,
                        onValueChange = { forbiddenItems = it },
                        label = { Text("Forbidden Content & Strict Banned Trademarks") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = preferredPalettes,
                        onValueChange = { preferredPalettes = it },
                        label = { Text("Preferred Color Palettes") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                SettingsEntity(
                                    allowedStyles = allowedStyles,
                                    forbiddenItems = forbiddenItems,
                                    preferredPalettes = preferredPalettes,
                                    ollamaEndpoint = ollamaUrl,
                                    ollamaModel = ollamaModel
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_specs_button")
                    ) {
                        Text("Save Design Specifications")
                    }
                }
            }
        }

        // Self-Hosted Open-Weight LLM Configuration
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("llm_config_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Self-Hosted Open-Weight AI Server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Connects to your local or always-free cloud VM Ollama / vLLM endpoint. Pure open-weight models (Llama 3, Mistral, Qwen). No hosted proprietary AI APIs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = ollamaUrl,
                        onValueChange = { ollamaUrl = it },
                        label = { Text("Ollama Base URL (e.g. http://10.0.2.2:11434)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ollamaModel,
                        onValueChange = { ollamaModel = it },
                        label = { Text("Model Name (e.g. llama3, mistral:7b-instruct-q4_K_M)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.updateSettings(
                                SettingsEntity(
                                    allowedStyles = allowedStyles,
                                    forbiddenItems = forbiddenItems,
                                    preferredPalettes = preferredPalettes,
                                    ollamaEndpoint = ollamaUrl,
                                    ollamaModel = ollamaModel
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Server Connection")
                    }
                }
            }
        }

        // Organic Marketing Generator Studio (Organic Only - Zero Ad Spend)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("marketing_studio_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Organic Marketing Agent Studio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "100% Organic growth. Generates viral Pinterest pins, 15s TikTok/Reels video scripts, SEO tags, and email campaigns without spending a cent on ads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = marketingNicheInput,
                        onValueChange = { marketingNicheInput = it },
                        label = { Text("Target Niche or Product Campaign") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.generateMarketingKit(marketingNicheInput) },
                        modifier = Modifier.fillMaxWidth().testTag("generate_marketing_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Organic Campaign Bundle")
                    }

                    generatedMarketing?.let { bundle ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📌 Pinterest Rich Pin:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(bundle.pinTitle, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Text(bundle.pinDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("🎬 TikTok / Reels 15s Script:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Hook: \"${bundle.tikTokHook}\"", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Text(bundle.tikTokScript, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("🏷 13 High-Rank SEO Tags:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(bundle.seoTags.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = BrandEmerald)

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("✉ Email Subject:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(bundle.emailSubject, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // Complete 11 Autonomous Agent Team Overview
        item {
            Text(
                text = "Autonomous Agent Team (11 Specialists)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(agents) { agent ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(agent.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandEmerald.copy(alpha = 0.15f)
                        ) {
                            Text(agent.role, style = MaterialTheme.typography.labelSmall, color = BrandEmerald, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(agent.systemPrompt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
