# Librerías

## FlatLaf

Da a los botones, sliders y la lista de canciones un aspecto actual, en vez
del de serie de Swing.

El JAR **no se versiona**. `ejecutar-swing.ps1` lo baja solo de Maven Central
la primera vez y comprueba su SHA-1 antes de usarlo, así que normalmente no
hay que hacer nada.

Si prefieres bajarlo a mano:

    https://repo1.maven.org/maven2/com/formdev/flatlaf/3.7.2/flatlaf-3.7.2.jar

y dejarlo en `lib/flatlaf-3.7.2.jar`.

Si el JAR no está, el programa arranca igual con el aspecto por defecto de
Swing: la dependencia es opcional.
