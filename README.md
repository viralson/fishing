# 🎣 Łowienie – customowy system łowienia (Paper 26.2 / 26.3)

Pełna przebudowa wędkowania: 50 gatunków ryb, minigra holu, ulepszane wędki, przynęty, dynamiczny targ, zawody, akwarium i potwory z głębin. Wszystkie menu i wiadomości są w MiniMessage, z animowaną wodą i dźwiękami.

## Instalacja
1. Wrzuć wszystkie pliki do **głównego folderu** repo na GitHubie (razem z `.github/workflows/build.yml`).
2. Actions → Build Lowienie → pobierz artefakt `Lowienie` → plik `Lowienie-1.0.0.jar` wrzuć do `plugins/`.
3. Opcjonalnie: **Vault** + plugin ekonomii (EssentialsX, CMI…). Bez nich działa wbudowany portfel.

## Jak się łowi
Zarzucasz normalnie. Gdy ryba weźmie, na pasku akcji pojawia się **hol**:
`⟨▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌⟩  ◆◆◇◇  ❤❤❤`
- biały/żółty znacznik jeździ w lewo i prawo,
- **kucnij (Shift) albo kliknij LPM**, gdy jest na **zielonym**,
- ciemnozielony środek = **perfekcyjne trafienie**,
- ◆ = potrzebne trafienia (2–7, zależnie od rzadkości), ❤ = ile pomyłek wytrzyma żyłka.

Kolor paska bossa zdradza, co siedzi na haczyku, zanim to wyciągniesz.

## Funkcje (52)
**Ryby i połowy**
1. 50 gatunków ryb w 6 rzadkościach: Pospolita → Mityczna.
2. Gatunki zależne od wody: ocean, rzeka, bagno, zimne wody, ciepłe wody, jaskinie (Y<45).
3. Gatunki biorące tylko w dzień albo tylko w nocy.
4. Gatunki pogodowe: tylko w deszczu albo w burzy.
5. Losowa długość i waga każdej ryby.
6. Ocena 1–5 gwiazdek zależna od rozmiaru.
7. Minigra holu z ruchomym znacznikiem i zieloną strefą.
8. Trudność rośnie z rzadkością: szybszy znacznik, więcej trafień.
9. Ryby epickie i wyżej szarpią – strefa przesuwa się w trakcie holu.
10. Perfekcyjny hol (bez pomyłki): +1 gwiazdka i ×1.5 XP.
11. Seria połowów: co 10 udanych holi +1 szczęścia (do +5).
12. Śmieci (buty, patyki, butelki…) – im więcej szczęścia, tym rzadziej.
13. Skarby: pieniądze, diamenty, szmaragdy, zaczarowane księgi, przynęty, Serce Morza.
14. Potwory z głębin: Utopiec, Strażnik głębin, Bagienna wiedźma.
15. Bossy: **Król Topielców** i **Kraken** – z paskiem bossa i dużą nagrodą.
16. Podwójny połów (umiejętność, złota przynęta, ławice).
17. Jedzenie ryb daje efekty zależne od rzadkości (regeneracja, siła, odporność…).
18. Ryby to przedmioty z opisem (długość, waga, gwiazdki, kto złowił, data) – można nimi handlować.

**Wędki i przynęty**
19. 6 klas wędek: Zwykła → Bambusowa → Stalowa → Karbonowa → Tytanowa → Mityczna.
20. 4 cechy wędki: Szczęście, Szybkość, Kontrola (szerokość zielonej strefy), Żyłka (dozwolone pomyłki).
21. Warsztat wędki – ulepszanie każdej cechy za pieniądze, do limitu klasy.
22. Zaklęcia Szczęście morza i Przynęta dodają się do cech.
23. 7 przynęt: Robak, Kulka zanętowa, Błystka, Świecąca (bonus w nocy i w jaskiniach), Krwista (potwory ×3), Magiczna, Złota.
24. Wybór ulubionej przynęty; gdy się skończy, plugin bierze inną, którą masz.

**Rozwój**
25. Poziomy wędkarza (1–50) z XP i nagrodą pieniężną za każdy poziom.
26. 7 umiejętności, 1 punkt co poziom: Szczęściarz, Podwójny połów, Poszukiwacz skarbów, Handlarz, Cierpliwość, Łowca bestii, Oszczędny.
27. Reset umiejętności za pieniądze.
28. Atlas ryb: odkryte gatunki i podpowiedzi siedlisk nieodkrytych.
29. Kamienie milowe atlasu 25/50/75/100% (100% = Tytanowa wędka + ogłoszenie).
30. Rekordy osobiste dla każdego gatunku.
31. Rekordy serwera dla każdego gatunku, ogłaszane przy pobiciu.
32. Dziennik / statystyki: połowy, ucieczki, perfekcyjne hole, serie, skarby, potwory, zarobki.
33. Rankingi: poziom, złowione ryby, zarobek, najcięższa ryba (także gracze offline).

**Ekonomia**
34. Siatka: ryby trafiają do wirtualnej siatki (36 → 108 miejsc).
35. Sortowanie siatki: najnowsze, najcenniejsze, rzadkość, waga.
36. Siatka: LPM sprzedaj, PPM wyjmij, Shift+LPM do akwarium; klik ryby w ekwipunku wkłada ją do siatki.
37. Targ rybny z **dynamicznymi cenami**: im więcej sprzedajesz danego gatunku, tym niższa cena; popyt odbudowuje się z czasem.
38. **Ryba dnia** z ceną ×2.5.
39. Cena zależy od wagi, gwiazdek i popytu; umiejętność Handlarz daje bonus.
40. Legendarne i mityczne ryby są chronione przed przypadkową sprzedażą (wymagany Shift).
41. Szybka sprzedaż: `/ryby sprzedaj [wszystko]`.
42. Akwarium: wystaw do 27 ryb, które dają pasywny dochód (gromadzi się do 24 h).
43. Animowane akwarium: ryby pływają po zbiorniku, bąbelki lecą w górę.
44. Wędkarnia: wędki, przynęty oraz ulepszenia siatki i akwarium.

**Wydarzenia i zabawa**
45. Zadania dnia: 3 losowe na gracza (9 typów) + bonus za komplet.
46. Zawody wędkarskie: automatyczne lub ręczne, 3 tryby (najwięcej ryb, najcięższa ryba, punkty), pasek bossa na żywo, nagrody dla top 3.
47. Szał ryb: losowe wydarzenie – ryby biorą 2× szybciej, +3 szczęścia.
48. Ławice: przy wędkarzach pojawiają się bulgoczące miejsca; łowienie w nich daje +4 szczęścia i większą szansę na podwójny połów.
49. Prognoza: co bierze tu i teraz, plus szansa % na każdą rzadkość.
50. Oprawa: cząsteczki, tytuły, fajerwerki przy mitycznych, ogłoszenia na czacie z podglądem ryby po najechaniu.
51. Unikalne menu przystani z animowanymi falami i rybką pływającą pod pomostem.
52. Ustawienia: łowienie do siatki, dźwięki holu, wyrzucanie śmieci, podpowiedzi.

## Komendy
| Komenda | Opis |
|---|---|
| `/ryby` | Przystań (główne menu) |
| `/ryby sprzedaj [wszystko]` | Sprzedaż ryb z siatki i ekwipunku |
| `/ryby siatka / targ / atlas / akwarium` | Siatka, targ, atlas, akwarium |
| `/ryby zadania / wedkarnia / wedka / umiejetnosci` | Zadania, sklep, warsztat wędki, umiejętności |
| `/ryby top / zawody / prognoza / ustawienia / staty [gracz]` | Ranking, zawody, prognoza, ustawienia, statystyki |

**Admin** (`lowienie.admin`):
- `/ryby daj wedke <gracz> <zwykla|bambusowa|stalowa|karbonowa|tytanowa|mityczna>`
- `/ryby daj przynete <gracz> <robak|kulka|blystka|swietlik|krwista|magiczna|zlota> [ilość]`
- `/ryby daj rybe <gracz> <gatunek> [gwiazdki]`
- `/ryby zawody start [ilosc|waga|punkty] [minuty]` · `/ryby zawody stop`
- `/ryby szal [minuty|stop]` · `/ryby xp <gracz> <ilość>` · `/ryby reload`

Aliasy: `/lowienie`, `/wedkarstwo`, `/fish`, `/ryba`. Ilości przyjmują krótkie formy (`10k`, `2mln`).

## Gatunki (50)
| Rzadkość | Gatunki |
|---|---|
| Pospolite | Karp, Leszcz, Płotka, Ukleja, Okoń, Dorsz, Śledź, Makrela, Karaś, Sardynka, Stynka |
| Niepospolite | Szczupak, Sandacz, Lin, Pstrąg, Łosoś, Dorada, Flądra, Węgorz, Sum, Błazenek |
| Rzadkie | Tuńczyk, Jesiotr, Miecznik, Rozdymka, Głowacica, Ryba księżycowa, Ślepczyk jaskiniowy, Pirania, Burzowy pstrąg, Deszczówka |
| Epickie | Marlin błękitny, Rekin młot, Arapaima, Lodowa ryba, Węgorz elektryczny, Latimeria, Złota rybka, Ryba-duch |
| Legendarne | Wielki biały rekin, Królewski jesiotr, Kryształowy karp, Tęczowa ryba, Ognisty łosoś, Księżycowy węgorz |
| Mityczne | Smocza ryba, Ryba Posejdona, Pradawny kolakant, Gwiezdna płetwa, Złoty Król Karpi |

Dane zapisują się w `plugins/Lowienie/gracze/<uuid>.yml` oraz `global.yml` (rekordy, targ, ranking).
