# Settings Specification & Architecture Rules (settings-spec.md)

## 1. Project Overview & Tech Stack
- **Application Name**: AgentOps (Autonomous Zero-Cost Storefront & Multi-Agent Swarm)
- **Framework**: Jetpack Compose (Material Design 3)
- **Language**: Kotlin 2.0+
- **Architecture**: MVVM + Clean Architecture with Coroutines & StateFlow
- **Data Layer**: Room Database (`agent_biz.db`) with reactive `Flow` & DAO bindings
- **Security & Secrets**: Secure `SecretManager` repository with `BuildConfig` / `.env` fallback and masked memory overrides
- **Design Style**: Material 3 Dark & Dynamic Emerald Accent (`#10B981` / `#059669`), responsive cards, 48dp touch targets, test tags for automated verification

---

## 2. Master Functional Settings Inventory

### Category 1: Account & Business Profile
| Key / Field | Type | Default Value | Description | Storage Layer |
|---|---|---|---|---|
| `businessName` | String | "Autonomous Studio" | Display name for generated storefronts and invoices | Room (`business_settings`) |
| `operatorAlias` | String | "Lead Founder" | Operator name for human approval signatures | Room (`business_settings`) |
| `supportEmail` | String | "ops@autonomous.studio" | Customer contact email embedded in public storefronts | Room (`business_settings`) |
| `storefrontProvider` | String | "GitHub Pages + Stripe" | Default deployment platform for zero-cost stores | Room (`business_settings`) |
| `requireBiometricPin` | Boolean | false | App lock toggle protecting financial and API secrets | Room (`business_settings`) |

### Category 2: Appearance & Display
| Key / Field | Type | Default Value | Description | Storage Layer |
|---|---|---|---|---|
| `themeMode` | String | "Dark" | Theme mode ("System", "Dark", "Light") | Room (`business_settings`) |
| `amoledMode` | Boolean | true | Pure #000000 background for OLED battery savings | Room (`business_settings`) |
| `accentColor` | String | "Emerald" | UI primary color accent ("Emerald", "Cyber Blue", "Neon Violet", "Amber Gold") | Room (`business_settings`) |
| `compactDensity` | Boolean | false | Compact view mode for high-density monitoring | Room (`business_settings`) |
| `currencySymbol` | String | "USD ($)" | Selected currency for ledger and product listings | Room (`business_settings`) |
| `uiAnimationsEnabled` | Boolean | true | Micro-interactions and animated status pulses | Room (`business_settings`) |

### Category 3: Autonomous Agent Swarm & Orchestration
| Key / Field | Type | Default Value | Description | Storage Layer |
|---|---|---|---|---|
| `globalAutomationActive` | Boolean | true | Master killswitch for autonomous background agent execution | Room (`business_settings`) |
| `productFrequency` | String | "Daily" | Agent dispatch schedule ("Every 1h", "Every 4h", "Every 12h", "Daily", "Weekly") | Room (`business_settings`) |
| `riskLevel` | String | "Balanced" | Agent operational risk ("Conservative", "Balanced", "Aggressive") | Room (`business_settings`) |
| `minMarginPercent` | Int | 35 | Minimum required net profit margin threshold (10% - 90%) | Room (`business_settings`) |
| `autoPitchGeneration` | Boolean | true | Agents proactively brainstorm pitches for review | Room (`business_settings`) |
| `maxDailyCreations` | Int | 8 | Upper bound of digital products created per cycle | Room (`business_settings`) |
| `enableAgentChatLogs` | Boolean | true | Verbose agent-to-agent deliberation logging | Room (`business_settings`) |
| `bypassApprovals` | Boolean | false | Auto-approve proposals (Testing/Sandbox only) | Room (`business_settings`) |
| `bypassRateLimits` | Boolean | false | Fallback immediately to Standby Pool when rate limited | Room (`business_settings`) |
| `offlineSimulationMode` | Boolean | false | Run all actions safely without live network calls | Room (`business_settings`) |

### Category 4: Per-Agent AI Model Matrix (Free Tier & Local)
| Key / Field | Type | Default Value | Description | Storage Layer |
|---|---|---|---|---|
| `modelCeo` | String | "gemini-2.5-flash" | Strategic orchestration and priority ranking | Room (`business_settings`) |
| `modelResearch` | String | "llama-3-70b" | Market trend and digital product ideation | Room (`business_settings`) |
| `modelProduct` | String | "mistral-large" | Digital product bundling and format packaging | Room (`business_settings`) |
| `modelMarketing` | String | "llama-3-8b" | Copywriting, storefront headlines, tags | Room (`business_settings`) |
| `modelContent` | String | "groq-mixtral" | High-speed asset and code snippet generation | Room (`business_settings`) |
| `modelSupport` | String | "gemini-2.5-flash" | Customer FAQ and automated returns handling | Room (`business_settings`) |
| `modelFinance` | String | "deepseek-coder" | Whole-cent double-entry ledger audits | Room (`business_settings`) |

### Category 5: Storefront & Zero-Cost eCommerce
| Key / Field | Type | Default Value | Description | Storage Layer |
|---|---|---|---|---|
| `autoDeployOnApproval` | Boolean | true | Trigger GitHub commit & Stripe link creation immediately upon approval | Room (`business_settings`) |
| `instantStripeCheckout` | Boolean | true | Automatically bundle Stripe Buy Button code in HTML | Room (`business_settings`) |
| `githubRepoVisibility` | String | "public" | Visibility of created storefront repositories ("public", "private") | Room (`business_settings`) |
| `digitalProductFormats` | String | "Markdown Guides, SVG Vectors, Code Snippets, Presets, Boilerplates" | Allowed file formats for zero-cost digital downloads | Room (`business_settings`) |
| `podProviderSandbox` | String | "Direct JAMStack Mock" | Mock/Live print on demand fulfillment connector | Room (`business_settings`) |

### Category 6: Trademark, Copyright & Legal Protection
| Key / Field | Type | Default Value | Description | Storage Layer |
|---|---|---|---|---|
| `strictCopyrightCheck` | Boolean | true | Mandatory pre-check before proposing to human | Room (`business_settings`) |
| `trademarkConfidenceMin` | Int | 98 | Required clearance safety threshold percentage (90% - 99%) | Room (`business_settings`) |
| `autoClearanceCertificate` | Boolean | true | Generate cryptographic SHA-256 clearance certificate | Room (`business_settings`) |
| `forbiddenItems` | String | "Trademarked logos, Disney/Marvel characters, celebrity names, copyrighted song lyrics, brand slogans" | Strict banned keywords list | Room (`business_settings`) |
| `allowedStyles` | String | "Minimalist line art, Vintage botanical, Retro typographic 70s, Cyber-grid geometric, Cottagecore watercolor" | Whitelisted aesthetic styles | Room (`business_settings`) |
| `preferredPalettes` | String | "Earthy terracotta, Charcoal slate, Pastel sage, Amber gold, Monochrome ink" | Allowed design color palettes | Room (`business_settings`) |

### Category 7: SecretManager & API Keys
| Key / Field | Type | Default Value | Description | Storage Layer |
|---|---|---|---|---|
| `keyGithub` | String | "" | GitHub Personal Access Token (repo, workflow scope) | SecretManager + Room (`business_settings`) |
| `keyStripe` | String | "" | Stripe Secret Key (`sk_test_...` or `sk_live_...`) | SecretManager + Room (`business_settings`) |
| `keyStripePublishable` | String | "" | Stripe Publishable Key (`pk_test_...`) | SecretManager + Room (`business_settings`) |
| `keyGemini` | String | "" | Google Gemini AI Key | SecretManager + Room (`business_settings`) |
| `keyGroq` | String | "" | Groq High-Speed LPU API Key | SecretManager + Room (`business_settings`) |
| `keyHuggingFace` | String | "" | Hugging Face User Access Token | SecretManager + Room (`business_settings`) |
| `keyOpenRouter` | String | "" | OpenRouter Unified API Key | SecretManager + Room (`business_settings`) |
| `backendServerEndpoint` | String | "http://10.0.2.2:8080" | Optional custom backend relay endpoint | Room (`business_settings`) |
| `ollamaEndpoint` | String | "http://10.0.2.2:11434" | Local Ollama inference URL | Room (`business_settings`) |

### Category 8: Push & In-App Notifications
| Key / Field | Type | Default Value | Description | Storage Layer |
|---|---|---|---|---|
| `pushNotificationsActive` | Boolean | true | Master toggle for push and system bar alerts | Room (`business_settings`) |
| `notificationLevel` | String | "All Alerts" | Filter tier ("All Alerts", "Approvals Only", "Silent") | Room (`business_settings`) |
| `notifyPitches` | Boolean | true | Alert when agents brainstorm a validated product pitch | Room (`business_settings`) |
| `notifySales` | Boolean | true | Immediate alert on recorded digital product or dropship sale | Room (`business_settings`) |
| `notifyTrademarkAlerts` | Boolean | true | Warning alert if an idea violates trademark rules | Room (`business_settings`) |
| `soundEnabled` | Boolean | true | Play audio chime on high-priority events | Room (`business_settings`) |
| `vibrationEnabled` | Boolean | true | Haptic feedback on button interactions & sales alerts | Room (`business_settings`) |

### Category 9: Privacy, Security & Data Management
| Key / Field | Type | Default Value | Description | Storage Layer |
|---|---|---|---|---|
| `analyticsTelemetry` | Boolean | false | Opt-out of anonymous usage diagnostics | Room (`business_settings`) |
| `localOnlyStorage` | Boolean | true | Guarantee all data stays strictly on-device in Room SQLite | Room (`business_settings`) |
| `autoBackupEnabled` | Boolean | true | Automatically snapshot state after each sale | Room (`business_settings`) |

---

## 3. State Management & Data Flow Architecture
1. **Source of Truth**: Single row `id = 1` in Room `business_settings` table.
2. **Reactive Flow**: `SettingsDao.getSettings()` exposes `Flow<SettingsEntity?>`.
3. **ViewModel Integration**: `MainViewModel.settings` collects this flow as `StateFlow<SettingsEntity?>` using `WhileSubscribed(5000)`.
4. **Optimistic & Local UI State**: The `SettingsScreen` binds local mutable states (`remember(currentSettings)`) to allow instant responsive toggling and editing with debounce/instant save triggers.
5. **SecretManager Synchronization**: When API keys are updated in the settings screen, `MainViewModel.saveSettings(...)` and `SecretManager.updateKeys(...)` synchronously update memory caches and database persistence.
6. **Persistence Assurance**: User choices survive app restarts, background killing, and configuration changes.
