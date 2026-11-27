# Audios embarqués

Convention : `<catégorie>/<langue>/<catégorie>_<langue>_<nn>.wav`

- **catégorie** : à quoi sert l'audio (`greetings` = accueil quand le robot détecte quelqu'un)
- **langue** : code de l'app — `fr`, `en`, `es`, `de`, `ar`, `ru` (pas `al` pour l'allemand)
- **nn** : numéro sur 2 chiffres (`01`, `02`…) ; plusieurs fichiers d'une même langue = variantes tirées au hasard

| Fichier | Contenu |
|---|---|
| `greetings/<langue>/greetings_<langue>_01.wav` | Accueil, quand le robot détecte quelqu'un (réglable dans Configuration du robot) |
| `formula_intro/<langue>/formula_intro_<langue>_01.wav` | Quiz : formule finale affichée — présente les boutons (imprimer, QR, mail, Emmène-moi, Terminer) |
| `guide_start/<langue>/guide_start_<langue>_01.wav` | Quiz : clic sur « Emmène-moi » — « Je vous y emmène, suivez-moi » |
| `guide_arrived/<langue>/guide_arrived_<langue>_01.wav` | Quiz : robot arrivé — « Nous y voilà, cliquez sur Terminer si nous avons fini » |
| `goodbye/<langue>/goodbye_<langue>_01.wav` | Quiz : clic sur « Terminer » — « Merci, passez une excellente journée » |

Les audios de fin de parcours ne sont joués qu'en **mode quiz** (en mode vocal, l'agent parle déjà).
Un nouvel audio coupe celui en cours. Dossier vide = silence (les `.gitkeep` sont ignorés).

Si une langue n'a pas de fichier, le français est utilisé.
Fichiers courts (2–5 s), sans silence au début. `.wav` ou `.mp3`.

Hors convention (historique) : `intro_{girl,boy}_{fr,en}.wav` à la racine.
