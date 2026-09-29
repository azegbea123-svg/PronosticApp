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
        "Malaga", "Real Zaragoza", "Deportivo La Coruna", "Sporting Gijon",

        // ── Championnats supplémentaires (ajoutés pour couvrir la diversité
        // des matchs sur lesquels les parieurs peuvent vouloir un pronostic,
        // au-delà des seuls grands championnats européens) ──
        // Premier Liga Kirghizistan
        "Bishkek-City", "Alga Bishkek", "Toktogul", "Aldier",

        // LaLiga
        "Levante", "RC Deportivo",

        // Serie A
        "Roma",

        // LaLiga2
        "Celta Fortuna", "FC Andorra", "Granada",

        // Clausura Argentina
        "Tigre", "Central Cordoba",

        // Liga MX Apertura
        "Pumas UNAM", "Necaxa",

        // Serie A Betano
        "Athletico Paranaense",

        // DFB Pokal
        "Altglienicke", "Verl", "Hamburger SV", "Wurzburger Kickers",
        "Koln", "Hallescher FC", "Schalke 04",

        // Liga Portugal
        "Gil Vicente", "Casa Pia AC",

        // Clausura Colombia
        "Junior", "Once Caldas",

        // Premier League Russie
        "Baltika Kaliningrad", "Rubin Kazan",

        // Super Lig
        "Kocaelispor", "Amedspor",

        // Liga de Primera Chili
        "Everton Vina del Mar", "Univ. Concepcion",

        // Allsvenskan
        "Malmo FF", "Djurgardens IF",

        // Ligue 2
        "Stade de Reims", "Annecy",

        // Greek Cup
        "Ellas Syrou", "Marko",

        // Superliga Roumanie
        "Botosani", "Csikszereda", "Otelul Galati", "FC Arges",

        // Liga de Expansion MX
        "CD Tapatio", "Mineros de Zacatecas",

        // Primera Nacional
        "Club Atletico Guemes", "Atletico Rafaela", "Ferro Carril Oeste", "All Boys",

        // Serie B Bresil
        "Novorizontino", "Sport Recife", "America Mineiro",

        // Efbet League
        "Botev Vratsa", "Botev Plovdiv", "Lokomotiv Plovdiv", "Arda Kardzhali",

        // Superliga Danemark
        "Brondby IF", "Silkeborg IF",

        // Liga Portugal 2
        "Farense", "Felgueiras 1932", "Feirense", "Porto II",

        // Eerste Divisie
        "Jong Utrecht", "Heracles", "Jong PSV", "TOP Oss",

        // Primera B Clausura
        "Tigres FC", "Union Magdalena", "Real Santander", "Independiente Valle Cauca",
        "Orsomarso SC", "Real Cundinamarca", "Leones", "Real Cartagena",
        "Patriotas Boyaca", "Atletico Futbol Club",

        // Lega Pro
        "Livorno", "Sassari Torres", "Reggiana", "Ravenna FC",
        "Folgore Caratese", "Dolomiti Bellunesi", "Salernitana", "Sorrento",
        "Team Altamura", "Monopoli", "Crotone", "Calcio Foggia",

        // Vysshaya Liga Bielorussie
        "Isloch", "Dinamo Minsk", "Torpedo-BelAZ", "Dnepr Mogilev",

        // FNL Russie
        "KamAZ", "Sochi", "Rotor Volgograd", "Neftekhimik",
        "Veles", "Arsenal Tula", "Nizhny Novgorod", "SKA-Khabarovsk",

        // USL Championship
        "Charleston Battery", "Miami FC",

        // 1. Lig Turquie
        "Sivasspor", "Manisa FK",

        // Liga de Ascenso Chili
        "San Luis de Quillota", "Santiago Wanderers",

        // Superettan
        "Ostersunds FK", "Ljungskile", "IFK Varnamo", "Landskrona BoIS",
        "IFK Norrkoping", "Falkenbergs",

        // Liga Nacional Guatemala
        "Marquense", "Coban Imperial", "Malacateco", "Dep. San Pedro",

        // 1. MFL Macedoine
        "FK Vardar", "Sileks", "KF Shkendija", "Struga",

        // Super League Ouzbekistan
        "Kokand 1912", "Buxoro",

        // Toplyga Lituanie
        "FK TransINVEST", "Siauliai",

        // Primera Division Costa Rica
        "LD Alajuelense", "San Carlos",

        // Liga Pro Equateur
        "Liga de Quito", "CS Emelec",

        // Saudi Professional League
        "Neom SC", "Al-Qadsiah FC", "Al-Ittihad", "Al-Hazem SC",

        // Ligat haAl Israel
        "Maccabi Netanya", "Bnei Sakhnin",

        // Primera C Metro Argentine
        "Centro Espanol", "Estrella del Sur", "Dep. Espanol", "Lujan",
        "Claypole", "Fenix Bs As",

        // Clausura Paraguay
        "Nacional", "Sportivo Luqueno", "Libertad", "Sportivo San Lorenzo",

        // Division 1 Suede
        "Hassleholms IF", "Laholms FK",

        // Vtora Liga Bulgarie
        "Vihren", "Fratria",

        // Besta deild karla Islande
        "Breidablik", "Fram",

        // Division 1 Pologne
        "Stal Rzeszow", "Ruch Chorzow",

        // Copa Simon Bolivar
        "Club Ingenieros", "Chaco Pando",

        // Torneo Promocional Amateur
        "SAT", "Atletico Pilar", "FC Ezeiza", "Belgrano Zarate",
        "Estrella de Berisso", "Nautico Hacoaj",

        // Super League Malaisie
        "Sabah", "Star City",

        // 2. Liga Slovaquie
        "Lokomotiva Zvolen", "FC Petrzalka",

        // Primera Apertura Guatemala
        "Sacachispas", "Achuapa", "Huehuetecos FC", "Quiche",

        // Liga Primera Nicaragua
        "Walter Ferretti", "UNAN Managua",

        // Esiliiga Estonie
        "FC Elva", "FC Flora Tallin II", "Viimsi JK", "Nomme United II",
        "Tallinna Kalev", "Maardu FC",

        // Nakotnes Liga Lettonie
        "Riga FC II", "Beitar/Riga Mariners",

        // Leumit League Israel
        "Maccabi Kiryat Gat FC", "Hapoel Rishon LeZion", "Maccabi Ahi Nazareth", "Hapoel Afula",
        "FC Ashdod", "Maccabi Kabilio Jaffa",

        // Division Intermedia Paraguay
        "Club 12 de Junio", "Paraguari AC",

        // Puerto Rico League
        "EF Taurinos Cayey", "Academia Quintana",

        // Persian Gulf Pro League
        "Tractor", "Persepolis Tehran", "Kheybar", "Peykan",
        "Malavan", "Fajr Sepasi", "Zob Ahan", "Mes Shahr Babak",
        "GolGohar", "Chadormalu", "Naft Abadan", "Foolad Khuzestan",

        // Ligi Kuu Bara Tanzanie
        "Young Africans", "JKT Tanzania", "Coastal Union", "TRA United",

        // Mozzart Bet Prva Liga Serbie
        "Smederevo", "Teleoptik", "FK Proleter 023", "Graficar",
        "FK Spartak Subotica", "Javor Ivanjica",

        // Panama Promotion League
        "Panama City", "UMECIT II",

        // Esiliiga B Estonie
        "Levadia III", "TJK Legion", "Vaprus Parnu II", "Tabasalu Charma",

        // Armenian Cup
        "Araks Ararat", "Cilicia",

        // Liga 2 Perou
        "Alianza Universidad", "Tacna Heroica",

        // Premier League 2 Singapour
        "Albirex Niigata II", "Young Lions II", "Tampines Rovers II", "Hougang Utd. II",

        // Division 2 Qatar
        "Al Mesaimeer", "Muaither", "Al Shamal II", "Al Kharitiyath",

        // Segunda Categoria Equateur
        "Vicentino Dragons", "Ciudad Canela",

        // Calcutta Premier Division
        "Kalighat Milan Sangha", "East Bengal Club", "Calcutta Police", "Kalighat SL",
        "Peerless SC", "Wari Athletic Club", "Techno Aryan", "Rainbow",

        // Sri Lanka League
        "Red Star", "Blue Eagles SC",
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
