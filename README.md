forge-description-plugin
========================

A plugin to add @Description annotation on java classes, interfaces, fields and methods.
Creates resource property files to host language values.

> git clone <this project>
> forge source-plugin forge-description-plugin
> cd tsheet <sample projec>
tsheet > description setup
tsheet > cd src/main/java/org/adorsys/tsheet/jpa/PersonJPA.java
PersonJPA > description add-class-description --value "The person class"
PersonJPA > description add-field-description --onProperty name --value "This person's name"
PersonJPA > description add-field-description --onProperty name --value "Le nom de cette personne" --locale fr