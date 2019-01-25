package io.github.benas.randombeans.validation;

//import javax.validation.constraints.Email;
//import javax.validation.constraints.Min;
//import javax.validation.constraints.NotNull;

public class Person {

//    @NotNull
    private String name;

//    @Email
    private String email;

//    @Min(10)
    private int age;

    public Person() {
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
