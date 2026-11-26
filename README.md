# Lylo Tablet (Android / Kotlin / Jetpack Compose)

Portage natif Android du front web Lylo (`lylo-front`, Next.js/React/Tailwind) pour une
utilisation en borne/tablette. Même design system, mêmes 7 écrans, même logique métier
(session IA vocale via LiveKit, quiz visuel, recommandations de formules, partage QR code),
reconstruite avec Kotlin + Jetpack Compose.

Ce document est honnête sur ce qui est fonctionnel dès l'ouverture du projet et sur ce qui
nécessite une configuration/vérification supplémentaire avant un run réel sur tablette —
voir la section **État d'avancement & limitations connues** à la fin.

---

## 1. Ouvrir le projet dans Android Studio

**Prérequis** : Android Studio Ladybug (2024.2) ou plus récent, JDK 17 (fourni par Android
Studio), Android SDK 35 (installé automatiquement au premier sync si absent).

1. `File → Open…` puis sélectionner le dossier `lylo-tablet/` (ce dossier, frère de
   `lylo-front/`).
2. Copier `local.properties.example` vers `local.properties` à la racine du projet et
   compléter les valeurs (voir section 2 ci-dessous). Android Studio y ajoutera aussi
   automatiquement la ligne `sdk.dir=...` au premier sync — ne la supprimez pas.
3. **Gradle Wrapper** : le fichier binaire `gradle/wrapper/gradle-wrapper.jar` n'est **pas**
   inclus (ce portage a été généré sans accès à un environnement Gradle/Android SDK pour le
   compiler ou le tester). `gradle-wrapper.properties` est présent et pointe vers Gradle
   8.10.2. Deux options :
   - Ouvrir simplement le projet dans Android Studio : il proposera de régénérer le wrapper
     automatiquement, ou
   - Lancer `gradle wrapper --gradle-version 8.10.2` depuis un Gradle installé localement à
     la racine du projet.
4. Laisser Android Studio synchroniser Gradle (télécharge les dépendances : Compose BOM,
   Navigation Compose, LiveKit Android SDK, Credential Manager, Coil, Retrofit, ZXing…).
5. Lancer sur un émulateur tablette (API 26+, paysage) ou une tablette physique via
   `Run ▶`.

**Aucune compilation réelle n'a pu être vérifiée dans cet environnement** (pas d'Android
SDK/Gradle disponible ici) — voir section 5 pour le détail de la méthode de relecture
utilisée à la place, et section « Limitations connues » pour les points les plus susceptibles
de nécessiter un ajustement mineur à la première compilation réelle.

---

## 2. Configuration (équivalent des variables d'environnement Next.js)

Le front web utilise des variables `NEXT_PUBLIC_*` dans `.env`. Côté Android, l'équivalent est
`local.properties` (non versionné) → injecté dans `BuildConfig` par `app/build.gradle.kts`.

| Variable web (`NEXT_PUBLIC_*`)     | Équivalent Android (`local.properties`) | Accès dans le code            | Valeur actuelle (reprise du `.env` de production lue dans `lylo-front/.env`) |
|-------------------------------------|------------------------------------------|--------------------------------|-------------------------------------------------------------------------------|
| `NEXT_PUBLIC_API_URL`                | `LYLO_API_BASE_URL`                       | `BuildConfig.API_BASE_URL`     | `https://lylo-back-production.up.railway.app` |
| `NEXT_PUBLIC_DEV_MODE`               | `LYLO_DEV_MODE`                           | `BuildConfig.DEV_MODE`         | `false` |
| `NEXT_PUBLIC_GOOGLE_CLIENT_ID`       | `LYLO_GOOGLE_WEB_CLIENT_ID`               | `BuildConfig.GOOGLE_WEB_CLIENT_ID` | `51707488835-jh0q9u70r28f2qu9ab5ee8k6abttp9d0.apps.googleusercontent.com` |

Ces valeurs par défaut sont **déjà câblées** dans `app/build.gradle.kts` (repli si
`local.properties` est absent), donc le projet compile "out of the box" avec la config de
production actuelle du backend Lylo — mais vous devriez les dupliquer dans votre propre
`local.properties` pour pouvoir les faire varier sans modifier le Gradle du projet (dev/staging).

### Important — Google OAuth Client ID

Le Client ID ci-dessus est un **Web Client ID** (type "Web application" dans Google Cloud
Console), pas un Client ID Android. C'est voulu : `androidx.credentials` +
`googleid.GetGoogleIdOption` exigent un `serverClientId` de type Web pour émettre un ID token
que le **même backend** (qui valide aujourd'hui les tokens du front React) puisse vérifier
sans modification côté serveur. Si votre projet Google Cloud a des restrictions
d'origine/package strictes, vous devrez peut-être y déclarer l'empreinte SHA-1 de signature de
l'app Android tablette dans un Client ID Android complémentaire (non requis avec
`GetGoogleIdOption` seul dans la plupart des configurations, mais à vérifier selon les
restrictions déjà en place sur ce projet Google Cloud).

### Google Fonts Provider (police EB Garamond)

`ui/theme/Type.kt` charge "EB Garamond" via le Google Fonts Provider
(`androidx.compose.ui.text.googlefonts`), qui nécessite Google Play Services sur l'appareil.
Le fichier `res/values/font_certs.xml` contient le tableau de certificats
`com_google_android_gms_fonts_certs` requis par ce provider — **ces empreintes ont été
reconstruites de mémoire dans cet environnement sans accès à un outil de génération officiel
(Android Studio "Downloadable Fonts" wizard) et devraient être régénérées/vérifiées** avant un
build de production, en suivant :
https://developer.android.com/develop/ui/views/text-and-emoji/downloadable-fonts
Si le provider échoue à résoudre la police (pas de Play Services, certificats invalides,
pas de réseau au premier lancement), Compose retombe silencieusement sur le typeface système
par défaut — pas de crash, mais un rendu non-serif tant que ce n'est pas corrigé.

---

## 3. Architecture du projet

```
app/src/main/java/com/lylo/tablet/
├── MainActivity.kt              // Point d'entrée, thème, permissions runtime, LyloApp()
├── LyloApplication.kt
├── auth/
│   └── GoogleSignInHelper.kt    // Credential Manager + Google Identity Services (remplace @react-oauth/google)
├── data/
│   ├── LocalPrefs.kt            // SharedPreferences = équivalent localStorage
│   ├── MockData.kt              // Formules/questions statiques (DEV_MODE)
│   ├── ShareableFormula.kt      // Encodage base64url pour le partage QR (lib/shareableFormula.ts)
│   ├── model/                   // SessionModels / AuthModels / QuizModels — miroir des types TS
│   └── remote/                  // Retrofit : ApiClient, LyloApiService, ApiModels (requêtes/réponses)
├── i18n/
│   ├── Language.kt              // AppLanguage (fr/en) + persistance (lib/language.ts)
│   └── LanguageViewModel.kt     // Switch dynamique de langue in-app (LanguageContext.tsx)
├── livekit/
│   ├── LiveKitSessionManager.kt // Connexion Room, data/control channels, transcriptions (LiveKitSession.tsx)
│   └── AvatarVideoTrack.kt      // Rendu de la piste vidéo de l'agent avatar (AvatarVideo.tsx)
├── navigation/
│   ├── LyloDestinations.kt      // Routes (miroir de src/app/*/page.tsx)
│   └── LyloNavGraph.kt          // NavHost Compose Navigation, câblage des 7 écrans + /formula
├── ui/
│   ├── components/              // Composants partagés (ChoiceCard, FormulaCard, BottomBar, TranscriptPanel...)
│   ├── screens/                 // Un package par écran : home, configure, quiz, preparation, interaction, recommendations, formula
│   └── theme/                   // Color.kt, Type.kt, Shape.kt, Effects.kt (glassPanel/cardShadow/aiGlow/micPulse), Theme.kt
└── viewmodel/
    ├── SessionViewModel.kt      // Portage complet de SessionContext.tsx (le plus gros/critique)
    ├── AuthViewModel.kt         // Portage de AuthContext.tsx
    ├── ConfigViewModel.kt       // État local de ConfigPanel.tsx
    ├── QuizViewModel.kt         // Machine à états complète du quiz (592 lignes source)
    ├── QuizResultsViewModel.kt  // Solo + multi results (599 lignes source)
    ├── ConnectionTestViewModel.kt
    └── MicCalibratorViewModel.kt
```

Les **contexts React** ont été portés en **ViewModels + StateFlow** (pas de
CompositionLocal pour l'état métier) — c'est l'équivalent idiomatique Android/Compose d'un
contexte global partagé, avec cycle de vie géré par le framework et pas de boilerplate de
"Provider" à empiler. `SessionViewModel` et `AuthViewModel` sont instanciés une seule fois
dans `MainActivity.LyloApp()` et transmis en paramètre à `LyloNavGraph`, exactement comme
`SessionProvider`/`AuthProvider` enveloppaient toute l'arborescence côté web.

`LanguageContext.tsx` (chaînes JSON dynamiques) est porté en **ressources Android standard**
(`res/values/strings.xml` = anglais, `res/values-fr/strings.xml` = français) + un
`LanguageViewModel` qui recrée un `Context` localisé à la volée pour permettre le switch
dynamique in-app (sélecteur FR/EN de la Navbar, LanguagePicker de `/configure`) sans redémarrer
l'app — combinant fidélité aux conventions Android et parité fonctionnelle avec le web.

---

## 4. Les 7 écrans (+ sous-routes)

| Route web              | Écran Compose                                  | État |
|-------------------------|-------------------------------------------------|------|
| `/`                     | `ui/screens/home/HomeScreen.kt`                  | Porté intégralement (Navbar, Hero, TechBar, lecture audio d'intro non portée — voir limitations) |
| `/configure`             | `ui/screens/configure/ConfigureScreen.kt` + `SelectorComponents.kt`, `LanguagePicker.kt`, `PrinterModal.kt` | Porté intégralement (mode voix/hybride/visuel, persona, profondeur, mode IA, avatar toggle, email récap, test connexion, imprimante, multi-participants avec couleurs) |
| `/quiz`                  | `ui/screens/quiz/QuizScreen.kt`                  | Porté intégralement (592 lignes source) — profil solo/multi étape par étape, questionnaire top2/bottom2/confirm, bascule multi-participants |
| `/quiz/results`          | `ui/screens/quiz/QuizResultsScreen.kt`           | Porté intégralement (599 lignes source) — solo + multi, sélection, sauvegarde, impression, QR code |
| `/recommendations`       | `ui/screens/recommendations/RecommendationsScreen.kt` | Porté intégralement (vue 1 formule / vue 2 formules, impression, QR code, BottomBar) |
| `/formula`               | `ui/screens/formula/FormulaScreen.kt`            | Porté intégralement — deep link `lylo://formula?data=...&lang=...` déclaré dans le Manifest |
| `/preparation`           | `ui/screens/preparation/PreparationScreen.kt`    | Porté intégralement (démarrage de session, avatar, conseils, connexion) |
| `/interaction`           | `ui/screens/interaction/InteractionScreen.kt`    | Porté intégralement (avatar + question + ChoiceGrid/AnswerConfirmation/IntensitySelector + BottomBar) |

Composants transverses portés : `ChoiceCard`, `ChoiceGrid`, `AvatarSection` (+ piste vidéo
LiveKit), `StepProgress`, `AnswerConfirmation`, `IntensitySelector`, `SpeakingIndicator`,
`FormulaCard`, `SizeToggle`, `FormulaQrCode` (génération QR **locale** via ZXing plutôt que
l'API externe `api.qrserver.com` utilisée côté web — fonctionne hors-ligne), `BottomBar`,
`TranscriptPanel`, `GeneratingLoader`, `ConnectionTest`, `MicCalibrator`, `Navbar`,
`LoginModal`.

**Non portés délibérément** (outils de debug web, hors périmètre de l'app tablette) :
`components/dev/DevNavigator.tsx` et `components/dev/DebugAudioOverlay.tsx` — panneaux de
navigation rapide entre écrans et d'inspection audio réservés au développement web, sans
équivalent utile sur un kiosque tablette en production.

---

## 5. Intégrations critiques

### LiveKit (`livekit/LiveKitSessionManager.kt`)

Utilise le SDK officiel **`io.livekit:livekit-android:2.13.0`**. Porte la logique de
`LiveKitSession.tsx` : connexion à la Room (`serverUrl`/`token` reçus de
`POST /api/session/start`), publication du micro local, écoute du data channel `"state"`
(relayée vers `SessionViewModel.handleDataMessage`, qui reproduit fidèlement le switch sur
tous les types d'événements : `profile_update`, `state_change`, `answer_saved`,
`formulas_generated`, `agent_state`, `waiting_for_top_2`/`bottom_2`,
`step_awaiting_confirmation`, `avatar_disabled`, etc.), envoi sur le control channel
`"control"` (réponses en mode clic, `interrupt`/`resume`/`resume_listen`), écoute des
transcriptions (`RoomEvent.TranscriptionReceived`), et un watchdog de timeout de connexion
(10s) avec retry unique, identique à la logique `handleConnectionTimeout()` du web.

**Honnêteté sur cette intégration** : le code utilise les parties bien documentées et
stables de l'API publique du SDK LiveKit Android (`LiveKit.create`, `Room.connect`,
`room.events` en tant que `Flow<RoomEvent>`, `localParticipant.setMicrophoneEnabled`,
`localParticipant.publishData`, `getTrackPublication(Track.Source.CAMERA)`). Cet
environnement de travail n'avait pas d'accès à un Gradle/Android SDK pour compiler
réellement le module et vérifier ces signatures contre la version exacte 2.13.0 — il est
possible qu'un détail de signature (nom de paramètre, type exact de retour) doive être
ajusté de quelques lignes à la première compilation réelle dans Android Studio. La structure
et la couverture fonctionnelle (tous les types d'events, tous les canaux, le retry) sont
en revanche complètes et fidèles à la version web — ce n'est pas un stub.

Non porté à l'identique : `BluetoothOutputEnforcer` (logique très spécifique au comportement
Chrome/WebRTC pour forcer la sortie audio sur un casque Bluetooth via `setSinkId`) n'a pas
d'équivalent direct dans le SDK LiveKit Android — sur Android, le routage audio Bluetooth/SCO
est généralement mieux géré nativement par le framework audio du système
(`AudioManager`/`AudioDeviceInfo`) qu'en WebView. Si des soucis de sortie audio Bluetooth sont
constatés en usage réel, il faudra ajouter une gestion explicite via `AudioManager` côté
Android (non implémentée ici, car sans accès à du matériel Bluetooth réel pour valider un tel
correctif il aurait été malhonnête de le simuler).

### Google OAuth (`auth/GoogleSignInHelper.kt`)

Remplace `@react-oauth/google` par **Credential Manager API** (`androidx.credentials`) +
**Google Identity Services / One Tap** (`com.google.android.libraries.identity.googleid`),
la voie officiellement recommandée par Google pour Android depuis la dépréciation de
Google Sign-In classique. `LoginModal.kt` déclenche `GoogleSignInHelper.signIn(context)`
au clic, récupère l'ID token, en extrait l'email (via `GoogleIdTokenCredential.id`, secouru
par un décodage JWT manuel équivalent à `decodeJwt()` côté web), puis appelle
`AuthViewModel.login(email)` qui interroge `GET /lookup?email=...` — flux identique au web.

### Permissions runtime

`AndroidManifest.xml` déclare `RECORD_AUDIO`, `CAMERA`, `MODIFY_AUDIO_SETTINGS`,
`BLUETOOTH_CONNECT`, `INTERNET`, etc. `MainActivity.kt` affiche un écran de demande de
permission (`PermissionGateScreen`) avant de monter le NavGraph si le micro n'est pas encore
accordé — équivalent du prompt navigateur déclenché implicitement par `getUserMedia()` côté
web, mais explicite et anticipé côté natif pour éviter un flash d'UI cassée en pleine session
vocale. `MicCalibrator` et `preparation` gèrent aussi leur propre demande de permission au cas
où l'utilisateur l'aurait refusée au lancement puis reviendrait dessus.

---

## 6. Méthode de relecture (pas de compilation réelle possible ici)

Aucun Android SDK / Gradle n'était disponible dans cet environnement de travail, donc **aucun
build réel n'a pu être exécuté**. À la place, une relecture systématique a été effectuée sur
les 60 fichiers Kotlin :
- vérification de l'équilibre des accolades et parenthèses sur chaque fichier ;
- vérification croisée que chaque `R.string.*`/`R.drawable.*`/`R.array.*` référencé dans le
  code Kotlin est bien déclaré dans les ressources (`strings.xml` fr/en, `drawable/`,
  `font_certs.xml`) ;
- relecture de chaque usage de `Modifier.weight()` pour confirmer qu'il s'exécute bien dans un
  `RowScope`/`ColumnScope` (piège classique en Compose) ;
- élimination des imports inutilisés et du code mort identifiés lors de la relecture ;
- vérification de la cohérence des noms de champs entre les modèles de données Kotlin
  (`data/model/*.kt`) et leurs usages dans les écrans/composants.

Cela **réduit fortement** le risque d'erreurs de compilation mais ne les élimine pas
totalement (une signature d'API tierce légèrement différente de ce qui est documenté, par
exemple, resterait invisible sans compilation réelle). Le premier `./gradlew assembleDebug`
dans Android Studio devra donc servir de validation finale, et il est probable (mais pas
garanti) qu'il passe sans erreur ou avec seulement quelques ajustements mineurs de signature
côté LiveKit/Credential Manager.

---

## 7. État d'avancement — ce qui est fonctionnel vs. ce qui reste à brancher

### Pleinement fonctionnel dès l'ouverture du projet (avec la config actuelle)
- Le design system complet (couleurs, typographie EB Garamond, radius, glass-panel,
  card-shadow, ai-glow, mic-pulse) porté fidèlement en thème Compose.
- Les 7 écrans + navigation Compose complète entre eux, avec tous les libellés UI repris
  du code source (FR par défaut si présent dans le JSON source, sinon EN).
- La persistance locale (SharedPreferences) de toute la config utilisateur (persona, langue,
  profondeur, mode, avatar, email récap, imprimante, participants/couleurs), miroir exact des
  clés `localStorage` utilisées côté web.
- Les appels API REST vers le backend Lylo réel (`https://lylo-back-production.up.railway.app`
  par défaut) : session start/get/delete, lookup auth, questions, génération de formules
  (solo/multi), sauvegarde, impression, test de connexion, liste des imprimantes.
- Le flux Google OAuth natif (Credential Manager) — fonctionnera si le Web Client ID actuel
  accepte les requêtes depuis une app Android (à vérifier/étendre dans Google Cloud Console
  si nécessaire, voir section 2).
- Le partage de formule par QR code, généré localement (ZXing), pointant vers `/formula` sur
  le front web existant (fonctionne aussi comme deep link natif `lylo://formula` si l'appli
  elle-même reçoit ce lien).

### Nécessite une vérification/config avant un run réel en production
- **LiveKit** : structure et couverture fonctionnelle complètes (voir section 5), mais jamais
  compilé réellement dans cet environnement — à valider en premier lors de l'ouverture du
  projet. Aucune clé LiveKit à configurer séparément : l'URL et le token sont fournis par le
  backend au moment de `POST /api/session/start`, exactement comme côté web.
- **Certificats Google Fonts Provider** (`font_certs.xml`) — reconstruits de mémoire, à
  régénérer via l'outil officiel Android Studio avant un build de production pour garantir le
  chargement d'EB Garamond (repli silencieux sur police système sinon, pas de crash).
- **Logo SDP** : remplacé par un placeholder simple (disque de couleur primary / icône
  vectorielle) dans `Navbar.kt` et `res/drawable/ic_launcher_foreground.xml` — à remplacer par
  le vrai `logo-sdp.png` du front web pour une fidélité pixel-perfect.
- **Avatars statiques** (`/avatar-f.jpg`, `/avatar-h.jpg` côté web) : remplacés par
  `res/drawable/avatar_placeholder.xml` (disque uni) — à remplacer par les vraies images.
- **Audio d'introduction** (`/intro-girl-fr.wav`, `/intro-boy-en.wav`) : le bouton
  "Introduction" de `HomeScreen.kt` bascule un état de lecture visuel mais ne joue **aucun
  son réel** — ces fichiers audio n'étaient pas inclus dans les dumps source lus et n'ont pas
  été portés en `res/raw/`. À ajouter en copiant les fichiers depuis `lylo-front/public/` et
  en câblant un `MediaPlayer`/`ExoPlayer` sur le clic.

### Limitations connues / écarts assumés par rapport au web
- Pas de vrai `backdrop-filter` CSS en Compose : `glassPanel()`/`glassNav()` approximent
  l'effet verre dépoli avec un fond blanc translucide plutôt qu'un flou du contenu
  sous-jacent — visuellement très proche sur les fonds utilisés dans l'app, mais pas
  physiquement identique pixel pour pixel.
- Le toggle "plein écran" du panneau de configuration web (API Fullscreen du navigateur) n'a
  pas d'équivalent direct utile côté natif — une app Android tourne déjà en plein écran par
  design ; ce contrôle a été volontairement omis plutôt que simulé sans effet réel. Un mode
  kiosque (Lock Task Mode) serait l'équivalent natif pertinent si un usage borne verrouillée
  est souhaité, non implémenté ici.
- `BluetoothOutputEnforcer` (forçage de sortie audio casque Bluetooth) non porté — voir
  section 5 pour le détail et la piste de correctif native (`AudioManager`).
- Les outils de debug web (`DevNavigator`, `DebugAudioOverlay`) n'ont pas d'équivalent —
  hors périmètre d'une app de production sur tablette.
- Le champ email de `ConfigPanel` (récap par email) est un simple `BasicTextField` sans
  clavier `KeyboardType.Email` explicite ni validation de format — fonctionnellement correct
  mais un peu moins guidé que l'`<input type="email">` HTML natif du web.

---

## 8. Dépendances Gradle principales ajoutées

Toutes déclarées dans `app/build.gradle.kts` :

- `androidx.navigation:navigation-compose` — navigation entre les 7 écrans
- `io.livekit:livekit-android` — SDK LiveKit officiel (audio/vidéo temps réel + data channels)
- `androidx.credentials:credentials` + `credentials-play-services-auth` +
  `com.google.android.libraries.identity.googleid:googleid` — Google OAuth natif
- `io.coil-kt.coil3:coil-compose` + `coil-network-okhttp` — chargement d'images distantes
  (photos des choix du quiz, avatar)
- `com.squareup.retrofit2:retrofit` + `converter-kotlinx-serialization` +
  `com.squareup.okhttp3:okhttp` (+ `logging-interceptor`) — client API REST
- `org.jetbrains.kotlinx:kotlinx-serialization-json` — sérialisation des modèles
- `com.google.zxing:core` — génération de QR code locale
- `androidx.datastore:datastore-preferences` — présent en dépendance pour extension future
  (l'app utilise actuellement `SharedPreferences` via `LocalPrefs.kt` pour coller au pattern
  synchrone de `localStorage`, voir section 3)
- `androidx.compose.ui:ui-text-google-fonts` — chargement de la police EB Garamond

---

## 9. Prochaines étapes suggérées

1. Ouvrir le projet dans Android Studio, laisser Gradle synchroniser, corriger les éventuelles
   erreurs de compilation résiduelles (attendues mineures, voir section 6).
2. Régénérer `font_certs.xml` via l'assistant officiel Android Studio.
3. Remplacer les placeholders visuels (logo, avatars, éventuellement l'icône de lancement)
   par les vrais assets du front web (`lylo-front/public/`).
4. Ajouter les fichiers audio d'introduction en `res/raw/` et câbler leur lecture.
5. Tester le flux LiveKit de bout en bout sur un vrai device (tablette) avec micro/caméra,
   contre le backend de production ou un environnement de test.
6. Vérifier la configuration Google Cloud Console pour le flux Credential Manager depuis une
   app Android (empreinte de signature si nécessaire selon les restrictions du projet).
