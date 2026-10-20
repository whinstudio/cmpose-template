package com.kturker.multiplatform.core.multilang.resource

import com.kturker.multiplatform.core.multilang.language.AppLanguage
import com.kturker.multiplatform.core.multilang.resource.res.languageResources

/**
 * The sharing screens' strings (spaces, members, invite links), one instance per language
 * ([forLanguage]), kept out of [StringResourcesUiModel] so an app without the backend has none of them. No default
 * values: a language that misses a string does not compile.
 */
data class SharingStrings(
    /** The spaces screen's title, and its entry on a blank app's start screen. */
    val spacesTitle: String,
    val newSpace: String,
    val spaceName: String,
    val create: String,
    val rename: String,
    val save: String,
    val delete: String,
    val leave: String,
    val cancel: String,
    val back: String,
    val close: String,
    /** Marks the space the app's screens show. */
    val selected: String,
    val select: String,
    val membersTitle: String,
    val roleOwner: String,
    val roleEditor: String,
    val roleViewer: String,
    /** After a member's name: the signed-in user themselves. */
    val you: String,
    val changeRole: String,
    val remove: String,
    val share: String,
    /** The role picker's title before an invite link is made. */
    val inviteAs: String,
    /** The share sheet's text; `{app}` is the app's name, `{url}` the invite link ([shareInviteText]). */
    val shareInvite: String,
    val deleteSpaceWarning: String,
    val leaveSpaceWarning: String,
    val removeMemberWarning: String,
    val inviteTitle: String,
    /** `{inviter}` and `{space}` ([inviteMessage]). */
    val invitedBy: String,
    /** `{space}`, for an inviter without a name ([inviteMessage]). */
    val invited: String,
    /** `{role}` ([inviteRoleText]). */
    val inviteRole: String,
    val accept: String,
    val decline: String,
    val inviteUsed: String,
    val inviteExpired: String,
    val inviteNotFound: String,
    /** A space that was deleted, or that the user was removed from (the server answers 404). */
    val errorNoAccess: String,
    /** The backend's rate limit (429 `RATE_LIMITED`): a minute later the call goes through again. */
    val errorRateLimited: String,
    val errorNetwork: String,
    val errorUnknown: String,
    /** Asks the server again after no connection or a server error. */
    val retry: String
) {
    companion object {
        private val EN = SharingStrings(
            spacesTitle = "Spaces",
            newSpace = "New space",
            spaceName = "Name",
            create = "Create",
            rename = "Rename",
            save = "Save",
            delete = "Delete",
            leave = "Leave",
            cancel = "Cancel",
            back = "Back",
            close = "Close",
            selected = "Selected",
            select = "Select",
            membersTitle = "Members",
            roleOwner = "Owner",
            roleEditor = "Editor",
            roleViewer = "Viewer",
            you = "(you)",
            changeRole = "Change role",
            remove = "Remove",
            share = "Share",
            inviteAs = "Invite as",
            shareInvite = "Join my list in {app}: {url}",
            deleteSpaceWarning = "This deletes the space and everything in it for all its members. This cannot be " +
                "undone.",
            leaveSpaceWarning = "You lose access to this space until someone invites you again.",
            removeMemberWarning = "They lose access to this space.",
            inviteTitle = "Invitation",
            invitedBy = "{inviter} invites you to join {space}.",
            invited = "You are invited to join {space}.",
            inviteRole = "Your role: {role}",
            accept = "Accept",
            decline = "Not now",
            inviteUsed = "This invite has already been used. Ask for a new link.",
            inviteExpired = "This invite has expired. Ask for a new link.",
            inviteNotFound = "Invite not found.",
            errorNoAccess = "You no longer have access to this space",
            errorRateLimited = "Too many requests. Please try again in a minute.",
            errorNetwork = "No connection",
            errorUnknown = "Something went wrong",
            retry = "Try again"
        )

        private val TR = SharingStrings(
            spacesTitle = "Alanlar",
            newSpace = "Yeni alan",
            spaceName = "Ad",
            create = "Oluştur",
            rename = "Yeniden adlandır",
            save = "Kaydet",
            delete = "Sil",
            leave = "Ayrıl",
            cancel = "Vazgeç",
            back = "Geri",
            close = "Kapat",
            selected = "Seçili",
            select = "Seç",
            membersTitle = "Üyeler",
            roleOwner = "Sahip",
            roleEditor = "Düzenleyen",
            roleViewer = "Görüntüleyen",
            you = "(sen)",
            changeRole = "Rolü değiştir",
            remove = "Çıkar",
            share = "Paylaş",
            inviteAs = "Davet edilecek rol",
            shareInvite = "{app} uygulamasındaki listeme katıl: {url}",
            deleteSpaceWarning = "Alan ve içindeki her şey tüm üyeleri için silinir. Geri alınamaz.",
            leaveSpaceWarning = "Biri seni yeniden davet edene kadar bu alana erişimini kaybedersin.",
            removeMemberWarning = "Bu alana erişimini kaybeder.",
            inviteTitle = "Davet",
            invitedBy = "{inviter} seni {space} alanına davet ediyor.",
            invited = "{space} alanına davet edildin.",
            inviteRole = "Rolün: {role}",
            accept = "Kabul et",
            decline = "Şimdi değil",
            inviteUsed = "Bu davet zaten kullanılmış. Yeni bir bağlantı iste.",
            inviteExpired = "Bu davetin süresi dolmuş. Yeni bir bağlantı iste.",
            inviteNotFound = "Davet bulunamadı.",
            errorNoAccess = "Bu alana artık erişimin yok",
            errorRateLimited = "Çok fazla istek. Lütfen bir dakika sonra yeniden dene.",
            errorNetwork = "Bağlantı yok",
            errorUnknown = "Bir şeyler ters gitti",
            retry = "Tekrar dene"
        )

        private val ES = SharingStrings(
            spacesTitle = "Espacios",
            newSpace = "Nuevo espacio",
            spaceName = "Nombre",
            create = "Crear",
            rename = "Cambiar nombre",
            save = "Guardar",
            delete = "Eliminar",
            leave = "Salir",
            cancel = "Cancelar",
            back = "Atrás",
            close = "Cerrar",
            selected = "Seleccionado",
            select = "Seleccionar",
            membersTitle = "Miembros",
            roleOwner = "Propietario",
            roleEditor = "Editor",
            roleViewer = "Lector",
            you = "(tú)",
            changeRole = "Cambiar rol",
            remove = "Quitar",
            share = "Compartir",
            inviteAs = "Invitar como",
            shareInvite = "Únete a mi lista en {app}: {url}",
            deleteSpaceWarning = "Esto elimina el espacio y todo su contenido " +
                "para todos sus miembros. No se puede deshacer.",
            leaveSpaceWarning = "Perderás el acceso a este espacio hasta que alguien te vuelva a invitar.",
            removeMemberWarning = "Perderá el acceso a este espacio.",
            inviteTitle = "Invitación",
            invitedBy = "{inviter} te invita a unirte a {space}.",
            invited = "Te han invitado a unirte a {space}.",
            inviteRole = "Tu rol: {role}",
            accept = "Aceptar",
            decline = "Ahora no",
            inviteUsed = "Esta invitación ya se ha usado. Pide un enlace nuevo.",
            inviteExpired = "Esta invitación ha caducado. Pide un enlace nuevo.",
            inviteNotFound = "No se encontró la invitación.",
            errorNoAccess = "Ya no tienes acceso a este espacio",
            errorRateLimited = "Demasiadas solicitudes. Inténtalo de nuevo en un minuto.",
            errorNetwork = "Sin conexión",
            errorUnknown = "Algo salió mal",
            retry = "Reintentar"
        )

        private val IT = SharingStrings(
            spacesTitle = "Spazi",
            newSpace = "Nuovo spazio",
            spaceName = "Nome",
            create = "Crea",
            rename = "Rinomina",
            save = "Salva",
            delete = "Elimina",
            leave = "Esci",
            cancel = "Annulla",
            back = "Indietro",
            close = "Chiudi",
            selected = "Selezionato",
            select = "Seleziona",
            membersTitle = "Membri",
            roleOwner = "Proprietario",
            roleEditor = "Editor",
            roleViewer = "Lettore",
            you = "(tu)",
            changeRole = "Cambia ruolo",
            remove = "Rimuovi",
            share = "Condividi",
            inviteAs = "Invita come",
            shareInvite = "Unisciti alla mia lista su {app}: {url}",
            deleteSpaceWarning = "Lo spazio e tutto il suo contenuto vengono eliminati " +
                "per tutti i membri. L’operazione non si può annullare.",
            leaveSpaceWarning = "Perderai l’accesso a questo spazio finché qualcuno non ti inviterà di nuovo.",
            removeMemberWarning = "Perderà l’accesso a questo spazio.",
            inviteTitle = "Invito",
            invitedBy = "{inviter} ti invita a unirti a {space}.",
            invited = "Sei stato invitato a unirti a {space}.",
            inviteRole = "Il tuo ruolo: {role}",
            accept = "Accetta",
            decline = "Non ora",
            inviteUsed = "Questo invito è già stato usato. Chiedi un nuovo link.",
            inviteExpired = "Questo invito è scaduto. Chiedi un nuovo link.",
            inviteNotFound = "Invito non trovato.",
            errorNoAccess = "Non hai più accesso a questo spazio",
            errorRateLimited = "Troppe richieste. Riprova tra un minuto.",
            errorNetwork = "Nessuna connessione",
            errorUnknown = "Qualcosa è andato storto",
            retry = "Riprova"
        )

        private val FR = SharingStrings(
            spacesTitle = "Espaces",
            newSpace = "Nouvel espace",
            spaceName = "Nom",
            create = "Créer",
            rename = "Renommer",
            save = "Enregistrer",
            delete = "Supprimer",
            leave = "Quitter",
            cancel = "Annuler",
            back = "Retour",
            close = "Fermer",
            selected = "Sélectionné",
            select = "Sélectionner",
            membersTitle = "Membres",
            roleOwner = "Propriétaire",
            roleEditor = "Éditeur",
            roleViewer = "Lecteur",
            you = "(vous)",
            changeRole = "Changer de rôle",
            remove = "Retirer",
            share = "Partager",
            inviteAs = "Inviter en tant que",
            shareInvite = "Rejoignez ma liste dans {app} : {url}",
            deleteSpaceWarning = "L’espace et tout son contenu sont supprimés " +
                "pour tous ses membres. Cette action est irréversible.",
            leaveSpaceWarning = "Vous perdez l’accès à cet espace jusqu’à ce que quelqu’un vous invite à nouveau.",
            removeMemberWarning = "Ce membre perd l’accès à cet espace.",
            inviteTitle = "Invitation",
            invitedBy = "{inviter} vous invite à rejoindre {space}.",
            invited = "Vous êtes invité à rejoindre {space}.",
            inviteRole = "Votre rôle : {role}",
            accept = "Accepter",
            decline = "Plus tard",
            inviteUsed = "Cette invitation a déjà été utilisée. Demandez un nouveau lien.",
            inviteExpired = "Cette invitation a expiré. Demandez un nouveau lien.",
            inviteNotFound = "Invitation introuvable.",
            errorNoAccess = "Vous n’avez plus accès à cet espace",
            errorRateLimited = "Trop de requêtes. Veuillez réessayer dans une minute.",
            errorNetwork = "Pas de connexion",
            errorUnknown = "Une erreur s’est produite",
            retry = "Réessayer"
        )

        /** The strings for an [AppLanguage] code; English for any other. */
        fun forLanguage(code: String): SharingStrings = when (code) {
            AppLanguage.TR.code -> TR
            AppLanguage.ES.code -> ES
            AppLanguage.IT.code -> IT
            AppLanguage.FR.code -> FR
            else -> EN
        }
    }
}

/**
 * The sharing screens' strings in the language of these app strings, so they follow the in-app language picker as
 * [StringResourcesUiModel] does: `LocalStringResources.current.sharing`.
 */
val StringResourcesUiModel.sharing: SharingStrings
    get() = SharingStrings.forLanguage(languageCode())

// The language whose strings these are (LanguageManager hands out the objects of resource/res); English otherwise
private fun StringResourcesUiModel.languageCode(): String =
    languageResources.entries.firstOrNull { it.value === this }?.key?.code ?: AppLanguage.EN.code

private const val APP = "{app}"
private const val URL = "{url}"
private const val INVITER = "{inviter}"
private const val SPACE = "{space}"
private const val ROLE = "{role}"

/** "Join my list in <app>: <url>", the text the share sheet hands to another app. */
fun SharingStrings.shareInviteText(
    appName: String,
    url: String
): String = shareInvite.replace(APP, appName).replace(URL, url)

/** Who invites the user to which space; without an inviter's name only the space. */
fun SharingStrings.inviteMessage(
    inviterName: String?,
    spaceName: String
): String = if (inviterName.isNullOrBlank()) {
    invited.replace(SPACE, spaceName)
} else {
    invitedBy.replace(INVITER, inviterName).replace(SPACE, spaceName)
}

/** "Your role: <role>"; [roleName] is already in the app's language. */
fun SharingStrings.inviteRoleText(roleName: String): String = inviteRole.replace(ROLE, roleName)
