# Audios embarqués

Convention : `<catégorie>/<langue>/<catégorie>_<langue>_<nn>.wav`

- **catégorie** : à quoi sert l'audio (`greetings` = accueil quand le robot détecte quelqu'un)
- **langue** : code de l'app — `fr`, `en`, `es`, `de`, `ar`, `ru` (pas `al` pour l'allemand)
- **nn** : numéro sur 2 chiffres (`01`, `02`…) ; plusieurs fichiers d'une même langue = variantes tirées au hasard

| Fichier | Contenu |
|---|---|
| `greetings/<langue>/greeting_<langue>_01.wav` | « Bonjour, voulez-vous créer votre parfum ? » |

Si une langue n'a pas de fichier, le français est utilisé.
Fichiers courts (2–5 s), sans silence au début. `.wav` ou `.mp3`.

Hors convention (historique) : `intro_{girl,boy}_{fr,en}.wav` à la racine.
