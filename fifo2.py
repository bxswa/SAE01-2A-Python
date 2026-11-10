def fifo(processus, nb_cpu, ram_totale, energie_par_cpu, energie):
    a_venir = list(processus)   
    file = []                   
    en_cours = []               
    ram_libre = ram_totale
    resultats = []              
    t = 0

    while t < len(energie) and (a_venir or file or en_cours):

        arrivants = [p for p in a_venir if p["date_soumission"] == t]
        arrivants.sort(key=lambda p: -p["priorite"])
        for p in arrivants:
            file.append(p)
            a_venir.remove(p)

        for e in en_cours[:]:
            if e["fin"] == t:
                ram_libre += e["ram"]
                en_cours.remove(e)

        nb_ok = min(nb_cpu, energie[t] // energie_par_cpu)

        for p in file[:]:
            if len(en_cours) >= nb_ok:
                break                       
            if p["ram"] <= ram_libre:       
                pris = [e["cpu"] for e in en_cours]
                for i in range(1, nb_cpu + 1):
                    if i not in pris:
                        cpu = i
                        break
                fin = t + p["duree"]
                en_cours.append({"id": p["id"], "ram": p["ram"], "fin": fin, "cpu": cpu})
                ram_libre -= p["ram"]
                file.remove(p)
                resultats.append((p["id"], t, fin, "CPU" + str(cpu)))

        t += 1

    return resultats