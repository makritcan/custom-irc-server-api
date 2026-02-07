@echo off
title IRC SERVER KONTROL
color 0A

echo ==================================================
echo IRC SERVER & WEB CHAT SISTEMI BASLATILIYOR
echo ==================================================
echo.
echo [BILGI] Sunucu su an baslatiliyor...
echo [BILGI] Kapatmak icin bu pencereyi (X) tusuna basarak kapatin.
echo.
echo Erisim Adresleri:
echo - Kullanici Paneli: http://localhost:8080/
echo - Admin Paneli:     http://localhost:8080/admin.html
echo - IRC Portu:        6667
echo.

:: Java calistirma
if exist "out\production\ircserver" (
    java -cp "out\production\ircserver" com.makrit.Main
) else (
    echo [HATA] Derlenmis dosyalar bulunamadi!
    echo Lutfen once IntelliJ IDEA uzerinden projeyi 'Build' yapin.
    echo (Ust menu: Build -> Build Project)
    echo.
    echo Eger 'out' klasoru yoksa bu hata normaldir.
    pause
    exit
)

pause
