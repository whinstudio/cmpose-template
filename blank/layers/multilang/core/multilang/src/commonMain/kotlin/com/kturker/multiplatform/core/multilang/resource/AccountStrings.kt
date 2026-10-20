package com.kturker.multiplatform.core.multilang.resource

import com.kturker.multiplatform.core.multilang.language.AppLanguage
import com.kturker.multiplatform.core.multilang.resource.res.languageResources

/**
 * The account screens' strings (sign-in, account settings, account deletion), one instance per language
 * ([forLanguage]), kept out of [StringResourcesUiModel] so an app without the backend has none of them. No default
 * values: a language that misses a string does not compile.
 */
data class AccountStrings(
    /** The account settings' title, and its entry on a blank app's start screen. */
    val accountTitle: String,
    val signIn: String,
    val sessionEnded: String,
    /** The sign-in screen without a Firebase config (google-services.json / GoogleService-Info.plist). */
    val firebaseUnavailable: String,
    val createAccount: String,
    val continueWithGoogle: String,
    val signInWithApple: String,
    val email: String,
    val password: String,
    val signOut: String,
    val deleteAccount: String,
    val deleteAccountWarning: String,
    /** [sharedSpacesDeleteWarning] for exactly one space; `{count}` stands for the number. */
    val sharedSpacesDeletedOne: String,
    /** [sharedSpacesDeleteWarning] for any other number; `{count}` stands for the number. */
    val sharedSpacesDeletedOther: String,
    val signInAgainToDelete: String,
    val delete: String,
    val back: String,
    val cancel: String,
    /** The name the server gives the user's own space, in the app's language. */
    val personalSpace: String,
    /** The backend's rate limit (429 `RATE_LIMITED`): a minute later the call goes through again. */
    val errorRateLimited: String,
    val errorNetwork: String,
    val errorInvalidCredentials: String,
    /** Firebase refused the Google or Apple account (not a typed password). */
    val errorProviderRejected: String,
    val errorEmailInUse: String,
    val errorWeakPassword: String,
    val errorUnknown: String
) {
    companion object {
        private val EN = AccountStrings(
            accountTitle = "Account",
            signIn = "Sign in",
            sessionEnded = "Your session ended. Please sign in again.",
            firebaseUnavailable = "Firebase is not configured. Add google-services.json and GoogleService-Info.plist " +
                "following the steps in the cmpose guide.",
            createAccount = "Create account",
            continueWithGoogle = "Continue with Google",
            signInWithApple = "Sign in with Apple",
            email = "Email",
            password = "Password",
            signOut = "Sign out",
            deleteAccount = "Delete account",
            deleteAccountWarning = "This deletes your account, your personal space and every space you own, with all " +
                "their content. This cannot be undone.",
            sharedSpacesDeletedOne = "{count} space you share with others will be deleted too, and its members lose " +
                "access.",
            sharedSpacesDeletedOther = "{count} spaces you share with others will be deleted too, and their members " +
                "lose access.",
            signInAgainToDelete = "Sign in again to delete your account",
            delete = "Delete",
            back = "Back",
            cancel = "Cancel",
            personalSpace = "Personal",
            errorRateLimited = "Too many requests. Please try again in a minute.",
            errorNetwork = "No connection",
            errorInvalidCredentials = "Wrong email or password",
            errorProviderRejected = "Could not sign in with this account",
            errorEmailInUse = "This email is already registered",
            errorWeakPassword = "Password must be at least 6 characters",
            errorUnknown = "Something went wrong"
        )

        private val TR = AccountStrings(
            accountTitle = "Hesap",
            signIn = "Giriş yap",
            sessionEnded = "Oturumun sona erdi. Lütfen yeniden giriş yap.",
            firebaseUnavailable = "Firebase yapılandırılmadı. cmpose kılavuzundaki adımlarla " +
                "google-services.json ve GoogleService-Info.plist ekle.",
            createAccount = "Hesap oluştur",
            continueWithGoogle = "Google ile devam et",
            signInWithApple = "Apple ile giriş yap",
            email = "E-posta",
            password = "Şifre",
            signOut = "Çıkış yap",
            deleteAccount = "Hesabı sil",
            deleteAccountWarning = "Hesabın, kişisel alanın ve sahibi olduğun tüm alanlar içerikleriyle birlikte " +
                "silinir. Geri alınamaz.",
            sharedSpacesDeletedOne = "Paylaştığın {count} alan da silinecek; üyeleri erişimini kaybeder.",
            sharedSpacesDeletedOther = "Paylaştığın {count} alan da silinecek; üyeleri erişimini kaybeder.",
            signInAgainToDelete = "Hesabı silmek için yeniden giriş yap",
            delete = "Sil",
            back = "Geri",
            cancel = "Vazgeç",
            personalSpace = "Kişisel",
            errorRateLimited = "Çok fazla istek. Lütfen bir dakika sonra yeniden dene.",
            errorNetwork = "Bağlantı yok",
            errorInvalidCredentials = "E-posta ya da şifre yanlış",
            errorProviderRejected = "Bu hesapla giriş yapılamadı",
            errorEmailInUse = "Bu e-posta zaten kayıtlı",
            errorWeakPassword = "Şifre en az 6 karakter olmalı",
            errorUnknown = "Bir şeyler ters gitti"
        )

        private val ES = AccountStrings(
            accountTitle = "Cuenta",
            signIn = "Iniciar sesión",
            sessionEnded = "Tu sesión ha terminado. Vuelve a iniciar sesión.",
            firebaseUnavailable = "Firebase no está configurado. Añade google-services.json y " +
                "GoogleService-Info.plist siguiendo los pasos de la guía de cmpose.",
            createAccount = "Crear cuenta",
            continueWithGoogle = "Continuar con Google",
            signInWithApple = "Iniciar sesión con Apple",
            email = "Correo electrónico",
            password = "Contraseña",
            signOut = "Cerrar sesión",
            deleteAccount = "Eliminar cuenta",
            deleteAccountWarning = "Se eliminarán tu cuenta, tu espacio personal y todos los espacios que te " +
                "pertenecen, con todo su contenido. No se puede deshacer.",
            sharedSpacesDeletedOne = "También se eliminará {count} espacio que compartes con otros; sus miembros " +
                "perderán el acceso.",
            sharedSpacesDeletedOther = "También se eliminarán {count} espacios que compartes con otros; sus miembros " +
                "perderán el acceso.",
            signInAgainToDelete = "Vuelve a iniciar sesión para eliminar tu cuenta",
            delete = "Eliminar",
            back = "Atrás",
            cancel = "Cancelar",
            personalSpace = "Personal",
            errorRateLimited = "Demasiadas solicitudes. Inténtalo de nuevo en un minuto.",
            errorNetwork = "Sin conexión",
            errorInvalidCredentials = "Correo o contraseña incorrectos",
            errorProviderRejected = "No se pudo iniciar sesión con esta cuenta",
            errorEmailInUse = "Este correo ya está registrado",
            errorWeakPassword = "La contraseña debe tener al menos 6 caracteres",
            errorUnknown = "Algo salió mal"
        )

        private val IT = AccountStrings(
            accountTitle = "Account",
            signIn = "Accedi",
            sessionEnded = "La sessione è scaduta. Accedi di nuovo.",
            firebaseUnavailable = "Firebase non è configurato. Aggiungi google-services.json e " +
                "GoogleService-Info.plist seguendo i passaggi della guida di cmpose.",
            createAccount = "Crea account",
            continueWithGoogle = "Continua con Google",
            signInWithApple = "Accedi con Apple",
            email = "Email",
            password = "Password",
            signOut = "Esci",
            deleteAccount = "Elimina account",
            deleteAccountWarning = "Verranno eliminati il tuo account, il tuo spazio personale e tutti gli spazi di " +
                "tua proprietà, con tutto il loro contenuto. L’operazione non può essere annullata.",
            sharedSpacesDeletedOne = "Verrà eliminato anche {count} spazio che condividi con altri; i suoi membri " +
                "perderanno l’accesso.",
            sharedSpacesDeletedOther = "Verranno eliminati anche {count} spazi che condividi con altri; i loro " +
                "membri perderanno l’accesso.",
            signInAgainToDelete = "Accedi di nuovo per eliminare il tuo account",
            delete = "Elimina",
            back = "Indietro",
            cancel = "Annulla",
            personalSpace = "Personale",
            errorRateLimited = "Troppe richieste. Riprova tra un minuto.",
            errorNetwork = "Nessuna connessione",
            errorInvalidCredentials = "Email o password errati",
            errorProviderRejected = "Impossibile accedere con questo account",
            errorEmailInUse = "Questa email è già registrata",
            errorWeakPassword = "La password deve contenere almeno 6 caratteri",
            errorUnknown = "Qualcosa è andato storto"
        )

        private val FR = AccountStrings(
            accountTitle = "Compte",
            signIn = "Se connecter",
            sessionEnded = "Votre session a expiré. Reconnectez-vous.",
            firebaseUnavailable = "Firebase n’est pas configuré. Ajoutez google-services.json et " +
                "GoogleService-Info.plist en suivant les étapes du guide cmpose.",
            createAccount = "Créer un compte",
            continueWithGoogle = "Continuer avec Google",
            signInWithApple = "Se connecter avec Apple",
            email = "E-mail",
            password = "Mot de passe",
            signOut = "Se déconnecter",
            deleteAccount = "Supprimer le compte",
            deleteAccountWarning = "Votre compte, votre espace personnel et tous les espaces dont vous êtes " +
                "propriétaire seront supprimés, avec tout leur contenu. Cette action est irréversible.",
            sharedSpacesDeletedOne = "{count} espace que vous partagez avec d’autres sera aussi supprimé ; ses " +
                "membres perdront l’accès.",
            sharedSpacesDeletedOther = "{count} espaces que vous partagez avec d’autres seront aussi supprimés ; " +
                "leurs membres perdront l’accès.",
            signInAgainToDelete = "Reconnectez-vous pour supprimer votre compte",
            delete = "Supprimer",
            back = "Retour",
            cancel = "Annuler",
            personalSpace = "Personnel",
            errorRateLimited = "Trop de requêtes. Veuillez réessayer dans une minute.",
            errorNetwork = "Pas de connexion",
            errorInvalidCredentials = "E-mail ou mot de passe incorrect",
            errorProviderRejected = "Connexion impossible avec ce compte",
            errorEmailInUse = "Cet e-mail est déjà utilisé",
            errorWeakPassword = "Le mot de passe doit contenir au moins 6 caractères",
            errorUnknown = "Une erreur s’est produite"
        )

        /** The strings for an [AppLanguage] code; English for any other. */
        fun forLanguage(code: String): AccountStrings = when (code) {
            AppLanguage.TR.code -> TR
            AppLanguage.ES.code -> ES
            AppLanguage.IT.code -> IT
            AppLanguage.FR.code -> FR
            else -> EN
        }
    }
}

/**
 * The account screens' strings in the language of these app strings, so they follow the in-app language picker as
 * [StringResourcesUiModel] does: `LocalStringResources.current.account`.
 */
val StringResourcesUiModel.account: AccountStrings
    get() = AccountStrings.forLanguage(languageCode())

// The language whose strings these are (LanguageManager hands out the objects of resource/res); English otherwise
private fun StringResourcesUiModel.languageCode(): String =
    languageResources.entries.firstOrNull { it.value === this }?.key?.code ?: AppLanguage.EN.code

private const val COUNT = "{count}"

/**
 * "N spaces you share will be deleted too", in the singular for exactly one. The account screen shows it only for
 * one or more spaces, where every supported language (EN, TR, ES, IT, FR) uses the singular for 1 alone.
 */
fun AccountStrings.sharedSpacesDeleteWarning(count: Int): String =
    (if (count == 1) sharedSpacesDeletedOne else sharedSpacesDeletedOther).replace(COUNT, count.toString())
