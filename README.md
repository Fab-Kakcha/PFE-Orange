# PFE-Orange

My first repository on GitHub

Commandes Utiles pour la conférence:

Se connecter sur la machine tomcat 10.184.155.57 (y mettre son html et son javascript dans /opt/application/64poms/products/apache-tomcat-7.0.23/webapps/docs/webRTC)

Pour voir les logs: cd  /opt/application/64poms/current/logs/now/

http://10.184.155.57:8080/docs/webRTC/indexFabrice.html

Lancer une conférence : admin svip confmanager conference dosyntfork start

Supprimer les fichiers conf\_1.rd and conf\_1.wr dans : /opt/application/64poms/current/tmp

Lancement script VM : /opt/testlab/utils/stageFabrice/src/main/java

Problème interface VM:
# rm /etc/udev/rules.d/70-persistent-net.rules
# reboot
# edit /etc/sysconfig/network-scripts/ifcfg-eth0
# service network restart

