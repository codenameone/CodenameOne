// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::annotation-sqlite-orm-java-001[]
package com.example;

import com.codename1.annotations.*;

@Entity(table = "users")
public class User {

    @Id(autoIncrement = true)
    public long id;

    @Column(name = "full_name", nullable = false)
    public String name;

    public int age;

    public java.util.Date createdAt;

    @DbTransient
    public String cacheKey;                            // <1>

    public User() { }
}
// end::annotation-sqlite-orm-java-001[]

// tag::annotation-sqlite-orm-java-002[]
import com.codename1.orm.EntityManager;
import com.codename1.orm.Dao;

EntityManager em = EntityManager.open("MyApp.db");
Dao<User> users = em.dao(User.class);

users.createTable();                                   // <1>

User u = new User();
u.name = "Alice";
u.age = 30;
users.insert(u);                                       // <2>

User found = users.findById(u.id);                     // <3>

for (User x : users.find("age > ?", 18)) {             // <4>
    // ...
}

u.age = 31;
users.update(u);                                       // <5>
users.delete(u);
em.close();
// end::annotation-sqlite-orm-java-002[]

// tag::annotation-sqlite-orm-java-003[]
em.beginTransaction();
try {
    users.insert(u1);
    users.insert(u2);
    em.commitTransaction();
} catch (IOException e) {
    em.rollbackTransaction();
    throw e;
}
// end::annotation-sqlite-orm-java-003[]
