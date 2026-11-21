#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Writes the storefront's interface text into web/StoreText.java.

    python3 storetext.py

The table below is the source of truth: one row per key, the English text and
its Spanish, French, German and Hebrew translations. Java sources must be
ASCII, so the generated arrays carry every other character as a backslash-u
escape. Add a row here, run this, and use the key in a template as
${page.t['key']} (StoreTextTest fails on a template key with no row).
A blank translation falls back to the English text.

These translations were written for the sample and have not been reviewed by
native speakers. Review them before shipping.
"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
TARGET = os.path.join(HERE, "src", "main", "java", "com", "codenameone", "examples",
                      "bazaar", "web", "StoreText.java")
LANGS = ["en", "es", "fr", "de", "he"]

T = [
# key, en, es, fr, de, he
("search", "Search", "Buscar", "Rechercher", "Suchen", "חיפוש"),
("cart", "Cart", "Carrito", "Panier", "Warenkorb", "עגלה"),
("menu", "Main menu", "Menú principal", "Menu principal", "Hauptmenü", "תפריט ראשי"),
("preview", "Theme preview: this draft is not live yet.", "Vista previa del tema: este borrador aún no está publicado.", "Aperçu du thème : ce brouillon n'est pas encore en ligne.", "Theme-Vorschau: Dieser Entwurf ist noch nicht live.", "תצוגה מקדימה של ערכת העיצוב: הטיוטה עדיין לא פורסמה."),
("footer.tagline", "Independent shops, one checkout.", "Tiendas independientes, un solo pago.", "Des boutiques indépendantes, un seul paiement.", "Unabhängige Shops, ein Checkout.", "חנויות עצמאיות, קופה אחת."),
("privacy", "Privacy and cookies", "Privacidad y cookies", "Confidentialité et cookies", "Datenschutz und Cookies", "פרטיות ועוגיות"),
("store.links", "Store", "Tienda", "Boutique", "Shop", "חנות"),
("chat.link", "Chat with the shop", "Chatea con la tienda", "Discuter avec la boutique", "Mit dem Shop chatten", "צ'אט עם החנות"),
("consent.ask", "This shop would like to use optional cookies for analytics and marketing. Necessary ones always load.", "Esta tienda quiere usar cookies opcionales para análisis y marketing. Las necesarias se cargan siempre.", "Cette boutique souhaite utiliser des cookies facultatifs pour les statistiques et le marketing. Les cookies nécessaires sont toujours chargés.", "Dieser Shop möchte optionale Cookies für Analyse und Marketing verwenden. Notwendige Cookies werden immer geladen.", "החנות מבקשת להשתמש בעוגיות אופציונליות לניתוח ולשיווק. עוגיות הכרחיות נטענות תמיד."),
("consent.all", "Accept all", "Aceptar todo", "Tout accepter", "Alle akzeptieren", "לקבל הכול"),
("consent.none", "Decline", "Rechazar", "Refuser", "Ablehnen", "לסרב"),
("consent.choose", "Choose", "Elegir", "Choisir", "Auswählen", "לבחור"),
("consent.analytics", "Analytics", "Análisis", "Statistiques", "Analyse", "ניתוח"),
("consent.marketing", "Marketing", "Marketing", "Marketing", "Marketing", "שיווק"),
("consent.save", "Save choices", "Guardar selección", "Enregistrer mes choix", "Auswahl speichern", "לשמור את הבחירות"),
("consent.declineAll", "Decline all", "Rechazar todo", "Tout refuser", "Alle ablehnen", "לסרב להכול"),
("cart.title", "Your cart", "Tu carrito", "Votre panier", "Ihr Warenkorb", "העגלה שלך"),
("cart.empty", "Your cart is empty.", "Tu carrito está vacío.", "Votre panier est vide.", "Ihr Warenkorb ist leer.", "העגלה שלך ריקה."),
("keepShopping", "Keep shopping", "Seguir comprando", "Continuer mes achats", "Weiter einkaufen", "להמשיך בקניות"),
("item", "Item", "Artículo", "Article", "Artikel", "פריט"),
("quantity", "Quantity", "Cantidad", "Quantité", "Menge", "כמות"),
("total", "Total", "Total", "Total", "Gesamt", "סה\"כ"),
("remove", "Remove", "Quitar", "Retirer", "Entfernen", "להסיר"),
("update", "Update", "Actualizar", "Mettre à jour", "Aktualisieren", "לעדכן"),
("preorder", "Pre-order", "Reservar", "Précommander", "Vorbestellen", "הזמנה מוקדמת"),
("subtotal", "Subtotal", "Subtotal", "Sous-total", "Zwischensumme", "סכום ביניים"),
("discount", "Discount", "Descuento", "Remise", "Rabatt", "הנחה"),
("tax", "Tax", "Impuestos", "Taxes", "Steuer", "מס"),
("tax.included", "Tax included", "Impuestos incluidos", "Taxes incluses", "inkl. Steuer", "כולל מס"),
("shipping", "Shipping", "Envío", "Livraison", "Versand", "משלוח"),
("checkOut", "Check out", "Pagar", "Passer la commande", "Zur Kasse", "לתשלום"),
("checkout", "Checkout", "Pago", "Paiement", "Kasse", "קופה"),
("checkout.invalid", "Please check the highlighted fields.", "Revisa los campos marcados.", "Veuillez vérifier les champs signalés.", "Bitte prüfen Sie die markierten Felder.", "נא לבדוק את השדות המסומנים."),
("contact", "Contact", "Contacto", "Coordonnées", "Kontakt", "פרטי קשר"),
("email", "Email", "Correo electrónico", "E-mail", "E-Mail", "דוא\"ל"),
("guest", "Checking out as a guest.", "Estás pagando como invitado.", "Vous commandez en tant qu'invité.", "Sie bestellen als Gast.", "התשלום מתבצע כאורח."),
("signIn", "Sign in", "Iniciar sesión", "Se connecter", "Anmelden", "כניסה"),
("guest.saved", "to use your saved addresses.", "para usar tus direcciones guardadas.", "pour utiliser vos adresses enregistrées.", "um Ihre gespeicherten Adressen zu verwenden.", "כדי להשתמש בכתובות השמורות שלך."),
("shipTo.legend", "Shipping address", "Dirección de envío", "Adresse de livraison", "Lieferadresse", "כתובת למשלוח"),
("saved", "Saved addresses", "Direcciones guardadas", "Adresses enregistrées", "Gespeicherte Adressen", "כתובות שמורות"),
("newAddress", "Enter a new address", "Introducir una dirección nueva", "Saisir une nouvelle adresse", "Neue Adresse eingeben", "להזין כתובת חדשה"),
("useAddress", "Use this address", "Usar esta dirección", "Utiliser cette adresse", "Diese Adresse verwenden", "להשתמש בכתובת זו"),
("fullName", "Full name", "Nombre completo", "Nom complet", "Vollständiger Name", "שם מלא"),
("street", "Street address", "Dirección", "Adresse", "Straße und Hausnummer", "כתובת רחוב"),
("line2", "Apartment, suite (optional)", "Piso, apartamento (opcional)", "Appartement, bureau (facultatif)", "Wohnung, Etage (optional)", "דירה, כניסה (לא חובה)"),
("city", "City", "Ciudad", "Ville", "Stadt", "עיר"),
("region", "State or region", "Provincia o región", "État ou région", "Bundesland oder Region", "מחוז או אזור"),
("postal", "Postal code", "Código postal", "Code postal", "Postleitzahl", "מיקוד"),
("country", "Country", "País", "Pays", "Land", "מדינה"),
("phoneOpt", "Phone (optional)", "Teléfono (opcional)", "Téléphone (facultatif)", "Telefon (optional)", "טלפון (לא חובה)"),
("updateDelivery", "Update delivery options", "Actualizar opciones de entrega", "Mettre à jour les options de livraison", "Lieferoptionen aktualisieren", "לעדכן אפשרויות משלוח"),
("company", "Company account", "Cuenta de empresa", "Compte entreprise", "Firmenkonto", "חשבון חברה"),
("netTerms", "Place on net terms", "Pedir con pago a plazo", "Commander avec paiement différé", "Auf Rechnung bestellen", "להזמין בתנאי תשלום דחויים"),
("payment", "Payment", "Pago", "Paiement", "Zahlung", "תשלום"),
("nothingDue", "Nothing is left to pay.", "No queda nada por pagar.", "Il ne reste rien à payer.", "Es ist nichts mehr zu zahlen.", "לא נותר סכום לתשלום."),
("newCard", "A new card", "Una tarjeta nueva", "Une nouvelle carte", "Eine neue Karte", "כרטיס חדש"),
("cardNumber", "Card number", "Número de tarjeta", "Numéro de carte", "Kartennummer", "מספר כרטיס"),
("cardHint", "Test mode: 4242 4242 4242 4242 pays; 4000 0000 0000 0002 is declined.", "Modo de prueba: 4242 4242 4242 4242 paga; 4000 0000 0000 0002 se rechaza.", "Mode test : 4242 4242 4242 4242 paie ; 4000 0000 0000 0002 est refusée.", "Testmodus: 4242 4242 4242 4242 zahlt; 4000 0000 0000 0002 wird abgelehnt.", "מצב בדיקה: 4242 4242 4242 4242 משלם; 4000 0000 0000 0002 נדחה."),
("expiry", "Expiry (MM/YY)", "Caducidad (MM/AA)", "Expiration (MM/AA)", "Gültig bis (MM/JJ)", "תוקף (MM/YY)"),
("cvc", "Security code", "Código de seguridad", "Code de sécurité", "Sicherheitscode", "קוד אבטחה"),
("note", "Note for the store (optional)", "Nota para la tienda (opcional)", "Note pour la boutique (facultatif)", "Nachricht an den Shop (optional)", "הערה לחנות (לא חובה)"),
("summary", "Order summary", "Resumen del pedido", "Récapitulatif de la commande", "Bestellübersicht", "סיכום ההזמנה"),
("delivery", "Delivery", "Entrega", "Livraison", "Lieferung", "משלוח"),
("enterAddress", "Enter an address to see delivery options.", "Introduce una dirección para ver las opciones de entrega.", "Saisissez une adresse pour voir les options de livraison.", "Geben Sie eine Adresse ein, um die Lieferoptionen zu sehen.", "יש להזין כתובת כדי לראות אפשרויות משלוח."),
("discountCode", "Discount code", "Código de descuento", "Code de réduction", "Rabattcode", "קוד הנחה"),
("apply", "Apply", "Aplicar", "Appliquer", "Anwenden", "להחיל"),
("giftCard", "Gift card", "Tarjeta regalo", "Carte cadeau", "Geschenkkarte", "כרטיס מתנה"),
("duty", "Import duties (estimate)", "Aranceles de importación (estimación)", "Droits d'importation (estimation)", "Einfuhrzölle (Schätzung)", "מכס יבוא (הערכה)"),
("toPay", "To pay", "A pagar", "À payer", "Zu zahlen", "לתשלום"),
("dutyNote", "Duties are an estimate the carrier collects on delivery; they are not part of this charge.", "Los aranceles son una estimación que el transportista cobra en la entrega; no forman parte de este cargo.", "Les droits sont une estimation perçue par le transporteur à la livraison ; ils ne font pas partie de ce paiement.", "Zölle sind eine Schätzung, die der Zusteller bei Lieferung erhebt; sie sind nicht Teil dieser Zahlung.", "המכס הוא הערכה שחברת השילוח גובה במסירה; הוא אינו חלק מחיוב זה."),
("placeOrder", "Place order", "Realizar pedido", "Passer la commande", "Bestellung aufgeben", "לבצע הזמנה"),
("backToCart", "Back to cart", "Volver al carrito", "Retour au panier", "Zurück zum Warenkorb", "חזרה לעגלה"),
("err.discount", "Enter a discount code", "Introduce un código de descuento", "Saisissez un code de réduction", "Geben Sie einen Rabattcode ein", "יש להזין קוד הנחה"),
("err.gift", "Enter a gift card code", "Introduce el código de la tarjeta regalo", "Saisissez le code de la carte cadeau", "Geben Sie den Code der Geschenkkarte ein", "יש להזין קוד כרטיס מתנה"),
("err.email", "Enter the email address the receipt goes to", "Introduce el correo al que se enviará el recibo", "Saisissez l'adresse e-mail qui recevra le reçu", "Geben Sie die E-Mail-Adresse für die Quittung ein", "יש להזין את כתובת הדוא\"ל לקבלה"),
("err.country", "Choose a country", "Elige un país", "Choisissez un pays", "Wählen Sie ein Land", "יש לבחור מדינה"),
("err.name", "Enter the name the order goes to", "Introduce el nombre del destinatario", "Saisissez le nom du destinataire", "Geben Sie den Namen des Empfängers ein", "יש להזין את שם הנמען"),
("err.line1", "Enter the street address", "Introduce la dirección", "Saisissez l'adresse", "Geben Sie Straße und Hausnummer ein", "יש להזין את כתובת הרחוב"),
("err.city", "Enter the city", "Introduce la ciudad", "Saisissez la ville", "Geben Sie die Stadt ein", "יש להזין את העיר"),
("err.rate", "The store does not deliver to that address", "La tienda no entrega en esa dirección", "La boutique ne livre pas à cette adresse", "Der Shop liefert nicht an diese Adresse", "החנות אינה שולחת לכתובת זו"),
("err.card", "Enter the card number", "Introduce el número de tarjeta", "Saisissez le numéro de carte", "Geben Sie die Kartennummer ein", "יש להזין את מספר הכרטיס"),
("err.expiry", "Enter the expiry date as MM/YY", "Introduce la caducidad como MM/AA", "Saisissez la date d'expiration au format MM/AA", "Geben Sie das Ablaufdatum als MM/JJ ein", "יש להזין תוקף בתבנית MM/YY"),
("err.cvc", "Enter the card's security code", "Introduce el código de seguridad de la tarjeta", "Saisissez le code de sécurité de la carte", "Geben Sie den Sicherheitscode der Karte ein", "יש להזין את קוד האבטחה של הכרטיס"),
("reviews.from", "from", "de", "sur", "aus", "מתוך"),
("reviews", "reviews", "reseñas", "avis", "Bewertungen", "ביקורות"),
("market", "Market", "Mercado", "Marché", "Markt", "שוק"),
("option", "Option", "Opción", "Option", "Option", "אפשרות"),
("choose", "Choose", "Elegir", "Choisir", "Auswählen", "לבחור"),
("howToBuy", "How to buy", "Cómo comprar", "Mode d'achat", "Kaufart", "אופן הרכישה"),
("oneTime", "One-time purchase", "Compra única", "Achat unique", "Einmalkauf", "רכישה חד-פעמית"),
("addToCart", "Add to cart", "Añadir al carrito", "Ajouter au panier", "In den Warenkorb", "להוסיף לעגלה"),
("soldOut", "Sold out", "Agotado", "Épuisé", "Ausverkauft", "אזל מהמלאי"),
("specs", "Specifications", "Especificaciones", "Caractéristiques", "Technische Daten", "מפרט"),
("reviews.title", "Reviews", "Reseñas", "Avis", "Bewertungen", "ביקורות"),
("verified", "Verified purchase", "Compra verificada", "Achat vérifié", "Verifizierter Kauf", "רכישה מאומתת"),
("related", "You may also like", "También te puede gustar", "Vous aimerez aussi", "Das könnte Ihnen auch gefallen", "אולי יעניין אותך גם"),
("products", "products", "productos", "produits", "Produkte", "מוצרים"),
("sort", "Sort", "Ordenar", "Trier", "Sortieren", "מיון"),
("noMatch", "Nothing matches.", "No hay resultados.", "Aucun résultat.", "Keine Treffer.", "אין תוצאות."),
("pages", "Pages", "Páginas", "Pages", "Seiten", "עמודים"),
("collections", "Collections", "Colecciones", "Collections", "Kollektionen", "קולקציות"),
("featured", "Featured", "Destacados", "À la une", "Empfohlen", "מומלצים"),
("emptyShelves", "Nothing on the shelves yet.", "Todavía no hay nada en los estantes.", "Rien en rayon pour l'instant.", "Noch nichts im Regal.", "המדפים עדיין ריקים."),
("fromBlog", "From the blog", "Del blog", "Sur le blog", "Aus dem Blog", "מהבלוג"),
("about", "About", "Acerca de", "À propos", "Über uns", "אודות"),
("thanks", "Thank you! Order", "¡Gracias! Pedido", "Merci ! Commande", "Vielen Dank! Bestellung", "תודה! הזמנה"),
("confirmSent", "A confirmation was sent to", "Se envió una confirmación a", "Une confirmation a été envoyée à", "Eine Bestätigung wurde gesendet an", "אישור נשלח אל"),
("placed", "Placed", "Realizado", "Passée le", "Aufgegeben", "בוצעה"),
("shipTo", "Ship to", "Enviar a", "Livrer à", "Lieferung an", "משלוח אל"),
("tracking", "Tracking", "Seguimiento", "Suivi", "Sendungsverfolgung", "מעקב"),
("invoice", "Download invoice (PDF)", "Descargar factura (PDF)", "Télécharger la facture (PDF)", "Rechnung herunterladen (PDF)", "להוריד חשבונית (PDF)"),
("via", "via", "con", "via", "über", "באמצעות"),
("st.packed", "Packed", "Empaquetado", "Emballé", "Verpackt", "נארז"),
("st.shipped", "Shipped", "Enviado", "Expédié", "Versandt", "נשלח"),
("st.out", "Out for delivery", "En reparto", "En cours de livraison", "In Zustellung", "בדרך למסירה"),
("st.delivered", "Delivered", "Entregado", "Livré", "Zugestellt", "נמסר"),
("st.transit", "In transit", "En tránsito", "En transit", "Unterwegs", "בדרך"),
("st.picked", "Picked up", "Recogido", "Retiré", "Abgeholt", "נאסף"),
("st.cancelled", "Cancelled", "Cancelado", "Annulé", "Storniert", "בוטל"),
("order", "Order", "Pedido", "Commande", "Bestellung", "הזמנה"),
("visitStore", "Visit the store", "Visitar la tienda", "Visiter la boutique", "Shop besuchen", "לבקר בחנות"),
("chat.intro", "Ask anything about an order or a product. The shop answers here and by email.", "Pregunta lo que quieras sobre un pedido o un producto. La tienda responde aquí y por correo.", "Posez vos questions sur une commande ou un produit. La boutique répond ici et par e-mail.", "Fragen Sie alles zu einer Bestellung oder einem Produkt. Der Shop antwortet hier und per E-Mail.", "אפשר לשאול כל שאלה על הזמנה או מוצר. החנות עונה כאן ובדוא\"ל."),
("chat.keep", "Keep this page's address to come back to the conversation.", "Guarda la dirección de esta página para volver a la conversación.", "Conservez l'adresse de cette page pour revenir à la conversation.", "Bewahren Sie die Adresse dieser Seite auf, um zum Gespräch zurückzukehren.", "כדאי לשמור את כתובת הדף כדי לחזור לשיחה."),
("name", "Name", "Nombre", "Nom", "Name", "שם"),
("message", "Message", "Mensaje", "Message", "Nachricht", "הודעה"),
("send", "Send", "Enviar", "Envoyer", "Senden", "לשלוח"),
("cookiesApps", "Cookies and apps", "Cookies y aplicaciones", "Cookies et applications", "Cookies und Apps", "עוגיות ואפליקציות"),
("cookiesText", "Necessary cookies keep your cart and checkout working and always load. Analytics and marketing apps load only if you accept them, and never at checkout.", "Las cookies necesarias mantienen el carrito y el pago en funcionamiento y se cargan siempre. Las aplicaciones de análisis y marketing solo se cargan si las aceptas, y nunca en el pago.", "Les cookies nécessaires font fonctionner le panier et le paiement et sont toujours chargés. Les applications de statistiques et de marketing ne se chargent que si vous les acceptez, et jamais au paiement.", "Notwendige Cookies halten Warenkorb und Kasse funktionsfähig und werden immer geladen. Analyse- und Marketing-Apps laden nur, wenn Sie zustimmen, und nie an der Kasse.", "עוגיות הכרחיות שומרות על פעולת העגלה והקופה ונטענות תמיד. אפליקציות ניתוח ושיווק נטענות רק אם אישרת אותן, ולעולם לא בקופה."),
("noApps", "This shop uses no third-party apps.", "Esta tienda no usa aplicaciones de terceros.", "Cette boutique n'utilise aucune application tierce.", "Dieser Shop verwendet keine Apps von Drittanbietern.", "החנות אינה משתמשת באפליקציות צד שלישי."),
("yourChoice", "Your choice", "Tu elección", "Votre choix", "Ihre Auswahl", "הבחירה שלך"),
("blog", "Blog", "Blog", "Blog", "Blog", "בלוג"),
("taggedPosts", "Posts tagged", "Artículos con la etiqueta", "Articles avec l'étiquette", "Beiträge mit dem Schlagwort", "פוסטים עם התגית"),
("noPosts", "No posts yet.", "Todavía no hay artículos.", "Aucun article pour l'instant.", "Noch keine Beiträge.", "עדיין אין פוסטים."),
("subscribe", "Subscribe (Atom)", "Suscribirse (Atom)", "S'abonner (Atom)", "Abonnieren (Atom)", "הרשמה (Atom)"),
("allPosts", "All posts", "Todos los artículos", "Tous les articles", "Alle Beiträge", "כל הפוסטים"),
("index.title", "Independent shops", "Tiendas independientes", "Boutiques indépendantes", "Unabhängige Shops", "חנויות עצמאיות"),
("index.lede", "Every store on Bazaar, in one place.", "Todas las tiendas de Bazaar en un solo lugar.", "Toutes les boutiques de Bazaar, au même endroit.", "Alle Shops auf Bazaar an einem Ort.", "כל החנויות ב-Bazaar, במקום אחד."),
("index.none", "No shops are open yet.", "Todavía no hay tiendas abiertas.", "Aucune boutique n'est encore ouverte.", "Noch sind keine Shops geöffnet.", "עדיין אין חנויות פתוחות."),
("password", "Password", "Contraseña", "Mot de passe", "Passwort", "סיסמה"),
("twoFactor", "Enter the six-digit code from your authenticator app.", "Introduce el código de seis dígitos de tu aplicación de autenticación.", "Saisissez le code à six chiffres de votre application d'authentification.", "Geben Sie den sechsstelligen Code aus Ihrer Authenticator-App ein.", "יש להזין את הקוד בן שש הספרות מאפליקציית האימות."),
("code", "Code", "Código", "Code", "Code", "קוד"),
("verify", "Verify", "Verificar", "Vérifier", "Bestätigen", "לאמת"),
("newHere", "New here?", "¿Eres nuevo?", "Nouveau ici ?", "Neu hier?", "חדש כאן?"),
("createAccount", "Create an account", "Crear una cuenta", "Créer un compte", "Konto erstellen", "יצירת חשבון"),
("yourName", "Your name", "Tu nombre", "Votre nom", "Ihr Name", "השם שלך"),
("phone", "Phone", "Teléfono", "Téléphone", "Telefon", "טלפון"),
("passwordAgain", "Password again", "Repite la contraseña", "Confirmez le mot de passe", "Passwort wiederholen", "סיסמה שוב"),
("createAccountButton", "Create account", "Crear cuenta", "Créer le compte", "Konto erstellen", "ליצור חשבון"),
("haveAccount", "Already have an account?", "¿Ya tienes una cuenta?", "Vous avez déjà un compte ?", "Sie haben bereits ein Konto?", "כבר יש לך חשבון?"),
("twoStep", "Two-step verification", "Verificación en dos pasos", "Validation en deux étapes", "Zwei-Schritt-Bestätigung", "אימות דו-שלבי"),
("err.locked", "Too many wrong passwords for that account. Try again in a few minutes.", "Demasiadas contraseñas incorrectas para esa cuenta. Vuelve a intentarlo en unos minutos.", "Trop de mots de passe erronés pour ce compte. Réessayez dans quelques minutes.", "Zu viele falsche Passwörter für dieses Konto. Versuchen Sie es in ein paar Minuten erneut.", "יותר מדי סיסמאות שגויות לחשבון זה. אפשר לנסות שוב בעוד כמה דקות."),
("err.signin", "That email and password do not match an account.", "Ese correo y esa contraseña no corresponden a ninguna cuenta.", "Cet e-mail et ce mot de passe ne correspondent à aucun compte.", "Diese E-Mail und dieses Passwort gehören zu keinem Konto.", "הדוא\"ל והסיסמה אינם תואמים לחשבון."),
("err.code", "That code is not right, or has been used. Try the next one.", "Ese código no es correcto o ya se usó. Prueba con el siguiente.", "Ce code est incorrect ou a déjà été utilisé. Essayez le suivant.", "Dieser Code ist falsch oder wurde schon verwendet. Versuchen Sie den nächsten.", "הקוד שגוי או שכבר נעשה בו שימוש. אפשר לנסות את הקוד הבא."),
("err.signupEmail", "Enter your email address.", "Introduce tu correo electrónico.", "Saisissez votre adresse e-mail.", "Geben Sie Ihre E-Mail-Adresse ein.", "יש להזין את כתובת הדוא\"ל שלך."),
("err.short", "Choose a password of at least 10 characters.", "Elige una contraseña de al menos 10 caracteres.", "Choisissez un mot de passe d'au moins 10 caractères.", "Wählen Sie ein Passwort mit mindestens 10 Zeichen.", "יש לבחור סיסמה באורך 10 תווים לפחות."),
("err.mismatch", "The two passwords are not the same.", "Las dos contraseñas no coinciden.", "Les deux mots de passe ne sont pas identiques.", "Die beiden Passwörter stimmen nicht überein.", "שתי הסיסמאות אינן זהות."),
("err.failed", "That did not work.", "No ha funcionado.", "Cela n'a pas fonctionné.", "Das hat nicht geklappt.", "זה לא הצליח."),
("storePrices", "store prices", "precios de la tienda", "prix de la boutique", "Shoppreise", "מחירי החנות"),
]


def esc(s):
    out = []
    for ch in s:
        o = ord(ch)
        if ch == '"':
            out.append('\\"')
        elif ch == '\\':
            out.append('\\\\')
        elif o < 0x20 or o > 0x7e:
            out.append('\\u%04x' % o)
        else:
            out.append(ch)
    return ''.join(out)


def main():
    keys = [r[0] for r in T]
    dup = sorted(set(k for k in keys if keys.count(k) > 1))
    if dup:
        raise SystemExit("duplicate keys: %s" % dup)
    body = []
    for li, lang in enumerate(LANGS):
        body.append("    private static final String[] %s = {" % lang.upper())
        for r in T:
            if len(r) != 1 + len(LANGS):
                raise SystemExit("bad row %r" % (r,))
            text = r[1 + li]
            if text:
                body.append('        "%s", "%s",' % (r[0], esc(text)))
        body.append("    };")
        body.append("")
    with open(TARGET) as f:
        src = f.read()
    start = src.index("    // BEGIN GENERATED")
    end = src.index("    // END GENERATED")
    src = (src[:start] + "    // BEGIN GENERATED by storetext.py; edit the table there.\n"
           + "\n".join(body) + src[end:])
    with open(TARGET, "w") as f:
        f.write(src)


if __name__ == "__main__":
    main()
