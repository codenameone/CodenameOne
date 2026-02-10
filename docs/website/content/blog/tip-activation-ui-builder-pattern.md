---
title: 'TIP: Activation UI and the Builder Pattern'
slug: tip-activation-ui-builder-pattern
url: /blog/tip-activation-ui-builder-pattern/
original_url: https://www.codenameone.com/blog/tip-activation-ui-builder-pattern.html
aliases:
- /blog/tip-activation-ui-builder-pattern.html
date: '2017-10-02'
author: Shai Almog
---

![Header Image](/blog/tip-activation-ui-builder-pattern/tip.jpg)

I wrote [two posts](/blog/tip-send-device-activation-sms-via-twilio.html) about the [SMS activation process](/blog/tip-intercept-incoming-sms-on-android.html). In the first I discussed using the Twilio API via REST and in the second I discussed the native interfaces for SMS interception we can use in Android. Now it’s time to put this all together and create a single API that’s fluid. It should include the full UI process but be flexible enough to let you design your own experience.

### The Builder

I wanted to create a UI experience that’s pretty typical and standard while hiding a lot of nuance. My first intuition was to just derive `Form` and build the UI. But that’s not a good approach.

When you derive a component you expose the entire API of the base class onward, that creates a confusing UI API so I decided to expose a builder API which looks something like this:
    
    
    TwilioSMS smsAPI = TwilioSMS.create(accountSID, authToken, fromPhone);
    ActivationForm.create("Signup").
            show(s -> Log.p(s), smsAPI);

There are several things going on here. I wrapped the code from the [SMS rest calls](/blog/tip-send-device-activation-sms-via-twilio.html) in an API called `TwilioSMS`. This allows me to hide the details of sending an SMS from the `ActivationForm` UI class. You will notice I create an instance of this class using the `create` method which accepts the title of the form. I then have a `show` method which calls us back with the phone number when activation succeeds…​

Since this is a builder pattern you can customize all sorts of things:
    
    
    ActivationForm.create("Signup").
            codeDigits(5). // number of digits in the activation code sent via SMS
            enterNumberLabel(string). // text of the label above the number input
            includeFab(true). // true if a fab should be shown, by default a fab button will appear in Android only
            includeTitleBarNext(true). // true if a next arrow should appear in the title, by default this would appear in non-Android platforms
            show(s -> Log.p(s), smsAPI);

The neat thing is that you can’t do all sorts of things you aren’t supposed to do as the `ActivationForm` class derives from `Object` and has a private constructor.

The UI for the activation form shows a form on top of the current form with a number entry UI.

![The SMS activation UI/UX](/blog/tip-activation-ui-builder-pattern/sms-activation-signup.png)

Figure 1. The SMS activation UI/UX

![When clicking on the country code you are prompted with a list of countries](/blog/tip-activation-ui-builder-pattern/sms-activation-flags.png)

Figure 2. When clicking on the country code you are prompted with a list of countries

### Obtaining the list of Countries

I got the list of flags and countries from <https://mledoze.github.io/countries/>

I converted the flags SVG’s to PNG’s and added them all to the `flags.res` file. This is more efficient than opening multiple pngs from the system. I considered using the SVG files with [our transcoder](/blog/flamingo-svg-transcoder.html) but decided against it due to the immaturity and possible performance issues of the tool. Flags are sensitive things and a bug in the transcoder might mean we do something offensive.

I couldn’t skip the flags as they provide a visual element that changes the perception of the UI completely.

I considered shipping the app with the JSON data file but it includes a lot of information I don’t need and weighs 500kb. So I wrote a quick app that just printed out the data as arrays and I pasted them into the source file. This is an important step as the list might change and we’d need to go thru it again…​ This is how I parsed the JSON, I just ran this in the simulator and pasted the output into the Java source file:
    
    
    private static final CaseInsensitiveOrder cio = new CaseInsensitiveOrder();
    class Country implements Comparable<Country> {
        public final String name;
        public final String code;
        public final String flag;
        public final String isoCode2;
        public final String isoCode3;
    
        public Country(String name, String code, String flag, String isoCode2, String isoCode3) {
            this.name = name;
            this.code = code;
            this.flag = flag;
            this.isoCode2 = isoCode2;
            this.isoCode3 = isoCode3;
        }
    
        @Override
        public int compareTo(Country o) {
            return cio.compare(name, o.name);
        }
    }
    
    ArrayList<Country> con = new ArrayList<>();
    try {
        JSONParser p = new JSONParser();
        Map<String, Object> dat = p.parseJSON(new InputStreamReader(getResourceAsStream("/countries.json"))); __**(1)**
        List<Map<String, Object>> l = (List<Map<String, Object>>)dat.get("root");
        for(Map<String, Object> m : l) { __**(2)**
            List ll = ((List)m.get("callingCode"));
            if(ll != null && ll.size() > 0) {
                String name = (String)((Map)m.get("name")).get("common");
                String callingCode = (String)ll.get(0);
                String code = (String)m.get("cioc");
                String flag = null;
                if(code != null && code.length() > 0) {
                    flag = code.toLowerCase();
                }
                String isoCode2 = (String)m.get("cca2");
                String isoCode3 = (String)m.get("cca3");
                con.add(new Country(name, callingCode, flag, isoCode2, isoCode3)); __**(3)**
            }
        }
    } catch(IOException err) {
        Log.e(err);
    }
    
    Collections.sort(con); __**(4)**
    System.out.println("private static final String[] COUNTRY_NAMES = {");
    for(Country c : con) { __**(5)**
        System.out.println("    "" + c.name + "",");
    }
    System.out.println("};");
    System.out.println("private static final String[] COUNTRY_CODES= {");
    for(Country c : con) {
        System.out.println("    "" + c.code + "",");
    }
    System.out.println("};");
    System.out.println("private static final String[] COUNTRY_FLAGS = {");
    for(Country c : con) {
        if(c.flag == null) {
            System.out.println("    null,");
        } else {
            System.out.println("    "" + c.flag + "",");
        }
    }
    System.out.println("};");
    System.out.println("private static final String[] COUNTRY_ISO2 = {");
    for(Country c : con) {
        System.out.println("    "" + c.isoCode2 + "",");
    }
    System.out.println("};");
    System.out.println("private static final String[] COUNTRY_ISO3 = {");
    for(Country c : con) {
        System.out.println("    "" + c.isoCode3 + "",");
    }
    System.out.println("};");

Most of that code is pretty simple:

__**1** | I load the JSON file from the resources  
---|---  
__**2** | I loop over the entries within the JSON file one by one  
__**3** | I construct a `Country` object based on the entry data  
__**4** | `Country` is sortable thanks to the `Comparable` interface so I can just sort the list  
__**5** | Now I can just create the 5 String arrays I want  
  
Yes, I know I could have kept the `Country` object and worked with that instead of 5 String arrays…​ I might change to that in the future.

### Packaging it into a CN1LIB

Diamond surprised me a couple of weeks ago when he packaged my last post as [a cn1lib](https://github.com/diamonddevgroup/SMSInterceptor). I think that’s great if what you need is interception of SMS messages.

[This cn1lib](https://github.com/codenameone/SMSActivation/tree/master) is pretty different in its goals. I wanted to solve a very specific and restricted use case of validating a user with an SMS. So while we have some things in common with our cn1libs the basic premise is pretty far apart and it shows.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
