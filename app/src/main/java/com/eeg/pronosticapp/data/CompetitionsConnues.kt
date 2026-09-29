package com.eeg.pronosticapp.data

/**
 * Liste locale de competitions connues, utilisee pour l'autocompletion
 * pendant la saisie du type de rencontre - meme principe que
 * EquipesConnues pour les equipes : filtrage instantane, sans appel
 * reseau, et la saisie libre reste toujours possible pour n'importe
 * quelle competition absente de cette liste.
 */
object CompetitionsConnues {

    val liste: List<String> = listOf(
        "Ligue 1", "LaLiga", "Premier League",
        "Serie A", "Bundesliga", "Primeira Liga",
        "Ligue des Champions", "Coupe", "Amical",
        "Premier Liga Kirghizistan", "LaLiga2", "Clausura Argentina",
        "Liga MX Apertura", "Serie A Betano", "DFB Pokal",
        "Liga Portugal", "Clausura Colombia", "Premier League Russie",
        "Super Lig", "Liga de Primera Chili", "Allsvenskan",
        "Ligue 2", "Greek Cup", "Superliga Roumanie",
        "Liga de Expansion MX", "Primera Nacional", "Serie B Bresil",
        "Efbet League", "Superliga Danemark", "Liga Portugal 2",
        "Eerste Divisie", "Primera B Clausura", "Lega Pro",
        "Vysshaya Liga Bielorussie", "FNL Russie", "USL Championship",
        "1. Lig Turquie", "Liga de Ascenso Chili", "Superettan",
        "Liga Nacional Guatemala", "1. MFL Macedoine", "Super League Ouzbekistan",
        "Toplyga Lituanie", "Primera Division Costa Rica", "Liga Pro Equateur",
        "Saudi Professional League", "Ligat haAl Israel", "Primera C Metro Argentine",
        "Clausura Paraguay", "Division 1 Suede", "Vtora Liga Bulgarie",
        "Besta deild karla Islande", "Division 1 Pologne", "Copa Simon Bolivar",
        "Torneo Promocional Amateur", "Super League Malaisie", "2. Liga Slovaquie",
        "Primera Apertura Guatemala", "Liga Primera Nicaragua", "Esiliiga Estonie",
        "Nakotnes Liga Lettonie", "Leumit League Israel", "Division Intermedia Paraguay",
        "Puerto Rico League", "Persian Gulf Pro League", "Ligi Kuu Bara Tanzanie",
        "Mozzart Bet Prva Liga Serbie", "Panama Promotion League", "Esiliiga B Estonie",
        "Armenian Cup", "Liga 2 Perou", "Premier League 2 Singapour",
        "Division 2 Qatar", "Segunda Categoria Equateur", "Calcutta Premier Division",
        "Sri Lanka League", "Finale", "Coupe du monde",
        "Sélection nationale", "Éliminatoires"
    )

    /** Filtre local, insensible a la casse, sur ce que l'utilisateur a tape. */
    fun suggestions(saisie: String, limite: Int = 6): List<String> {
        if (saisie.length < 2) return emptyList()
        val recherche = saisie.trim().lowercase()
        return liste
            .filter { it.lowercase().contains(recherche) }
            .sortedBy { !it.lowercase().startsWith(recherche) }
            .take(limite)
    }
}
