package com.codename1.samples;


import static com.codename1.ui.CN.*;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Dialog;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import com.codename1.io.Log;
import com.codename1.ui.Toolbar;
import java.io.IOException;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.io.NetworkEvent;
import com.codename1.ui.AutoCompleteTextField;
import com.codename1.ui.CN;
import com.codename1.ui.list.DefaultListModel;
import java.util.Objects;
import java.util.Timer;

/**
 * This file was generated by <a href="https://www.codenameone.com/">Codename One</a> for the purpose 
 * of building native mobile applications using Java.
 */
public class AutocompleteAsyncTest {

    private Form current;
    private Resources theme;

    public void init(Object context) {
        // use two network threads instead of one
        updateNetworkThreadCount(2);

        theme = UIManager.initFirstTheme("/theme");

        // Enable Toolbar on all Forms by default
        Toolbar.setGlobalToolbar(true);

        // Pro only feature
        Log.bindCrashProtection(true);

        addNetworkErrorListener(err -> {
            // prevent the event from propagating
            err.consume();
            if(err.getError() != null) {
                Log.e(err.getError());
            }
            Log.sendLogAsync();
            Dialog.show("Connection Error", "There was a networking error in the connection to " + err.getConnectionRequest().getUrl(), "OK", null);
        });        
    }
    
    public void start() {
        if(current != null){
            current.show();
            return;
        }
        Form hi = new Form("Hi World", BoxLayout.y());
        DefaultListModel options = new DefaultListModel();
        class Query {
            Timer handle;
            String text;
            
            private void cancel() {
                if (handle != null) {
                    handle.cancel();
                }
                handle = null;
                text = null;
            }
            
            
        }
        Query query = new Query();
        
        String[] db = new String[]{
            "les Escaldes",
"Andorra la Vella",
"Umm al Qaywayn",
"Ras al-Khaimah",
"Khawr Fakkān",
"Dubai",
"Dibba Al-Fujairah",
"Dibba Al-Hisn",
"Sharjah",
"Ar Ruways",
"Al Fujayrah",
"Al Ain",
"Ajman",
"Adh Dhayd",
"Abu Dhabi",
"Zaranj",
"Taloqan",
"Shīnḏanḏ",
"Shibirghān",
"Shahrak",
"Sar-e Pul",
"Sang-e Chārak",
"Aībak",
"Rustāq",
"Qarqīn",
"Qarāwul",
"Pul-e Khumrī",
"Paghmān",
"Nahrīn",
"Maymana",
"Mehtar Lām",
"Mazār-e Sharīf",
"Lashkar Gāh",
"Kushk",
"Kunduz",
"Khōst",
"Khulm",
"Khāsh",
"Khanabad",
"Karukh",
"Kandahār",
"Kabul",
"Jalālābād",
"Jabal os Saraj",
"Herāt",
"Ghormach",
"Ghazni",
"Gereshk",
"Gardēz",
"Fayzabad",
"Farah",
"Kafir Qala",
"Charikar",
"Barakī Barak",
"Bāmyān",
"Balkh",
"Baghlān",
"Ārt Khwājah",
"Āsmār",
"Asadābād",
"Andkhōy",
"Bāzārak",
"Markaz-e Woluswalī-ye Āchīn",
"Saint John’s",
"The Valley",
"Sarandë",
"Kukës",
"Korçë",
"Gjirokastër",
"Elbasan",
"Burrel",
"Vlorë",
"Tirana",
"Shkodër",
"Patos Fshat",
"Lushnjë",
"Lezhë",
"Laç",
"Kuçovë",
"Krujë",
"Kavajë",
"Fier-Çifçi",
"Fier",
"Durrës",
"Berat",
"Kapan",
"Goris",
"Hats’avan",
"Artashat",
"Ararat",
"Yerevan",
"Ejmiatsin",
"Spitak",
"Sevan",
"Masis",
"Vanadzor",
"Gavarr",
"Hrazdan",
"Armavir",
"Gyumri",
"Ashtarak",
"Abovyan",
"Saurimo",
"Lucapa",
"Luau",
"Uíge",
"Soio",
"Nzeto",
"N’dalatando",
"Mbanza Congo",
"Malanje",
"Luanda",
"Caxito",
"Cabinda",
"Sumbe",
"Namibe",
"Menongue",
"Luena",
"Lubango",
"Longonjo",
"Lobito",
"Cuito",
"Huambo",
"Catumbela",
"Catabola",
"Camacupa",
"Caluquembe",
"Caála",
"Benguela",
"Zárate",
"Villa Ocampo",
"Villa Lugano",
"Villaguay",
"Villa Gesell",
"Tigre",
"Tandil",
"San Vicente",
"Santo Tomé",
"Santa Elena",
"San Pedro",
"San Luis del Palmar",
"San Lorenzo",
"San Javier",
"San Isidro",
"Saladas"

        };
        
        AutoCompleteTextField atf = new AutoCompleteTextField(options) {
            @Override
            protected boolean filter(String text) {
                if (text.length() == 0) {
                    options.removeAll();
                    if (query.handle != null) {
                        query.handle.cancel();
                    }
                    return true;
                }
                if (Objects.equals(query.text, text)) {
                    return false;
                }
                if (query.handle != null) {
                    query.handle.cancel();
                }
                
                query.text = text;
                query.handle = CN.setTimeout(1000, ()->{
                    query.handle = null;
                    query.text = null;
                    
                    options.removeAll();
                    for (String city : db) {
                        if (city.toLowerCase().startsWith(text.toLowerCase())) {
                            options.addItem(city);
                        }
                    }
                    updateFilterList();
                    
                });
                return false;
            }
            
        };
        hi.add(atf);
        hi.show();
    }

    public void stop() {
        current = getCurrentForm();
        if(current instanceof Dialog) {
            ((Dialog)current).dispose();
            current = getCurrentForm();
        }
    }
    
    public void destroy() {
    }

}
