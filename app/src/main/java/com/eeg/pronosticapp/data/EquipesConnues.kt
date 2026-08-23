package com.eeg.pronosticapp.data

/**
 * Liste locale d'équipes connues, utilisée pour l'autocomplétion pendant
 * la saisie (filtrage instantané, sans appel réseau).
 *
 * ⚠️ Ce n'est PAS une liste exhaustive de tous les clubs du monde (BeSoccer
 * en recense plus de 100 000, impossible à maintenir à la main). C'est une
 * sélection des championnats majeurs, pensée pour couvrir la grande
 * majorité des recherches réelles. La saisie libre reste toujours possible
 * pour n'importe quelle équipe absente de cette liste — elle n'est pas
 * bloquante, juste une aide.
 */
object EquipesConnues {

    val liste: List<String> = listOf(
        // Espagne — LaLiga
        "Real Madrid", "FC Barcelona", "Atletico Madrid", "Real Sociedad", "Real Betis",
        "Sevilla", "Villarreal", "Athletic Club", "Valencia", "Celta Vigo",
        "Osasuna", "Getafe", "Rayo Vallecano", "Mallorca", "Girona",
        "Las Palmas", "Alaves", "Espanyol", "Leganes", "Valladolid",

        // Angleterre — Premier League
        "Manchester City", "Arsenal", "Liverpool", "Manchester United", "Chelsea",
        "Tottenham Hotspur", "Newcastle United", "Aston Villa", "Brighton", "West Ham United",
        "Crystal Palace", "Everton", "Fulham", "Wolverhampton", "Brentford",
        "Nottingham Forest", "Bournemouth", "Leicester City", "Southampton", "Ipswich Town",

        // France — Ligue 1
        "Paris Saint-Germain", "Monaco", "Olympique de Marseille", "Lille", "Lyon",
        "Lens", "Rennes", "Nice", "Strasbourg", "Reims",
        "Toulouse", "Nantes", "Montpellier", "Brest", "Le Havre",

        // Allemagne — Bundesliga
        "Bayern Munich", "Bayer Leverkusen", "Borussia Dortmund", "RB Leipzig", "Union Berlin",
        "Eintracht Frankfurt", "Wolfsburg", "Freiburg", "Mainz", "Borussia Monchengladbach",
        "Werder Bremen", "FC Koln", "Hoffenheim", "Augsburg", "Stuttgart",

        // Italie — Serie A
        "Inter Milan", "AC Milan", "Juventus", "Napoli", "AS Roma",
        "Lazio", "Atalanta", "Fiorentina", "Bologna", "Torino",
        "Genoa", "Monza", "Udinese", "Sassuolo", "Empoli",

        // Portugal — Primeira Liga
        "Benfica", "FC Porto", "Sporting CP", "Braga", "Vitoria Guimaraes",

        // Pays-Bas — Eredivisie
        "Ajax", "PSV Eindhoven", "Feyenoord", "AZ Alkmaar", "FC Twente",

        // Belgique — Pro League
        "Club Brugge", "Anderlecht", "Genk", "Standard Liege", "Gent",

        // Turquie
        "Galatasaray", "Fenerbahce", "Besiktas", "Trabzonspor",

        // Écosse
        "Celtic", "Rangers",

        // Brésil
        "Flamengo", "Palmeiras", "Corinthians", "Sao Paulo", "Santos",
        "Gremio", "Internacional", "Atletico Mineiro", "Fluminense", "Botafogo",

        // Argentine
        "Boca Juniors", "River Plate", "Racing Club", "Independiente", "San Lorenzo",

        // Autres pays sud-américains
        "Colo-Colo", "Universidad de Chile", "Penarol", "Nacional Montevideo",

        // Mexique / USA
        "Club America", "Chivas Guadalajara", "Cruz Azul", "LA Galaxy", "Inter Miami",

        // Afrique
        "Al Ahly", "Zamalek", "Wydad Casablanca", "Raja Casablanca",

        // Moyen-Orient / Asie
        "Al Hilal", "Al Nassr", "Al Ittihad", "Urawa Red Diamonds",

        // Autres clubs européens notables
        "Red Bull Salzburg", "Dynamo Kyiv", "Shakhtar Donetsk", "Olympiacos",
        "Panathinaikos", "AEK Athens", "Zenit Saint-Petersburg", "Spartak Moscow",
        "Malaga", "Real Zaragoza", "Deportivo La Coruna", "Sporting Gijon"
    )

    /** Filtre local, insensible à la casse, sur ce que l'utilisateur a tapé. */
    fun suggestions(saisie: String, limite: Int = 6): List<String> {
        if (saisie.length < 2) return emptyList()
        val recherche = saisie.trim().lowercase()
        return liste
            .filter { it.lowercase().contains(recherche) }
            .sortedBy { !it.lowercase().startsWith(recherche) } // priorité à "commence par"
            .take(limite)
    }
}
