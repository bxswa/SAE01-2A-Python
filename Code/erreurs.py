"""
erreurs.py

Exceptions pour la partie lecture/écriture.
Elles servent a signaler clairement la nature d'un probleme.
"""


class ErreurLecture(Exception):
    """Probleme lors de la lecture d'un fichier d'entree."""
    pass


class ErreurFormat(Exception):
    """Le contenu d'un fichier ne respecte pas le format attendu."""
    pass


class ErreurEcriture(Exception):
    """Probleme lors de l'ecriture d'un fichier de sortie."""
    pass