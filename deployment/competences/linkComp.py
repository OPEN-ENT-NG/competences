 ## python archive.py -A localdev -P 5432 -DB ong -u web-education -srv ./srv/storage -pw We_1234

import argparse
from datetime import datetime
import psycopg2
import shutil
import os

nbCompetencesNotesAffected  = 0
nbWorksAffected = 0
valueToDelete = '796'
valueToMaintain = '815'

def closeConnection(sql, cur):
  if(sql):
    cur.close()
    sql.close()
    print('(' + str(datetime.now()) + ')' + " PostgreSQL connection is closed")
  return

def updateHomeWorkWithOnlyValueToDelete():
	global valueToDelete
	global valueToMaintain
	global nbWorksAffected
	print('[Start] Update devoirs with Only id_competence ' + str(valueToDelete))

	query = 'UPDATE notes.competences_devoirs SET id_competence = ' + valueToMaintain
	query = query +	' WHERE id_competence=' + valueToDelete +  ' AND id_devoir IN (SELECT id_devoir FROM '
	query = query +	' (SELECT id_devoir, count(*) as nb FROM notes.competences_devoirs '
	query = query +	'     WHERE id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'     group by id_devoir) as res '
	query = query +	'     WHERE res.nb < 2); '

	cur.execute(query)
	nbcount = cur.rowcount
	nbWorksAffected += nbcount
	print('[Done] ----- devoirs updated : ' +  str(nbcount))



def updateCompetenceNoteWithOnlyValueToDelete():
	global valueToDelete
	global valueToMaintain
	global nbCompetencesNotesAffected
	print('[Start] Update CompetencesNotes with Only id_competence ' + str(valueToDelete))

	query = 'UPDATE notes.competences_notes SET id_competence = ' + valueToMaintain
	query = query +	' WHERE id_competence=' + valueToDelete +  ' AND id_devoir IN (SELECT id_devoir FROM '
	query = query +	' (SELECT id_devoir, count(*) as nb FROM notes.competences_devoirs '
	query = query +	'     WHERE id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'     group by id_devoir) as res '
	query = query +	'     WHERE  res.nb < 2); '

	cur.execute(query)
	nbcount = cur.rowcount
	nbCompetencesNotesAffected += nbcount
	print('[Done] ----- Competences Notes updated : ' +  str(nbcount))

def getAllNiveauFinalToSet():
	global valueToDelete
	query = 'SELECT * FROM notes.competence_niveau_final  WHERE id_competence =' + str(valueToDelete)
	cur.execute(query)
	return cur.fetchall()

def getRowOnConflict():
	global valueToMaintain
	query = 'SELECT * FROM notes.competence_niveau_final  WHERE id_competence =' + str(valueToMaintain)
	cur.execute(query)
	return cur.fetchall()

def checkIfConflict(row, conflictTable):
	conflict = False
	for line in conflictTable :
		if(row[0] == line[0] and row[1] == line[1] and row[2] == line[2] and row[4] == line[4]):
			conflict = True
	return conflict

def setValuesToMax(a, b):
	print('setValuesToMax')
	maxVal = max(a[2], b[2])

	query = ' UPDATE notes.competence_niveau_final SET niveau_final = ' + str(maxVal)
	query = query  + ' WHERE id_periode = ' + str(b[0]) + " AND id_eleve= '" + str(b[1]) + "' AND id_matiere= '" + str(b[4]) + "' AND  id_competence = " + str(b[3])
	print(query)
	cur.execute(query)
	sql.commit()

def deleteNiveauCompNoteTODelete(b):
	query = ' DELETE FROM notes.competence_niveau_final '
	query += ' WHERE id_periode=' + str(b[0]) + " AND id_eleve= '" + str(b[1]) + "' AND id_matiere= '" + str(b[4]) + "' AND  id_competence = " + str(b[3])
	cur.execute(query)
	sql.commit()

def manageConflict(row, conflicTable):
	for line in conflicTable :
		if(row[0] == line[0] and row[1] == line[1] and row[2] == line[2] and row[4] == line[4]):
			setValuesToMax(row, line)
			deleteNiveauCompNoteTODelete(row)

def switchComNote(b):
	global valueToMaintain
	query = ' UPDATE notes.competence_niveau_final SET id_competence = ' + str(valueToMaintain)
	query += ' WHERE id_periode=' + str(b[0]) + " AND id_eleve= '" + str(b[1]) + "' AND id_matiere= '" + str(b[4]) + "' AND  id_competence = " + str(b[3])
	cur.execute(query)
	sql.commit()


def updateNiveauFinalWithOnlyValueToDelete():
	global valueToDelete
	global valueToMaintain
	global nbCompetencesNotesAffected
	levelToSet = getAllNiveauFinalToSet()
	levelOnConflict = getRowOnConflict()

	for row in levelToSet :
		if(checkIfConflict(row, levelOnConflict)) :
			manageConflict(row, levelOnConflict)
		else :
			switchComNote(row)



	print('[Done] ----- Niveau Finaux updated : ' +  str(len(levelToSet)))

def countCompetencesNotesWithNB(hi, lo):
	global valueToDelete
	global valueToMaintain

	query = 'SELECT count(*) '
	query = query +	' FROM notes.competences_notes '
	query = query +	' Where (id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain + ' ) AND id_devoir IN '
	query = query +	'    (SELECT id_devoir FROM '
	query = query +	'                        (SELECT id_devoir, count(*) as nb FROM notes.competences_devoirs '
	query = query +	'                         WHERE id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'                         group by id_devoir) as res '
	query = query +	'    WHERE res.nb > ' + str(lo) + ' AND res.nb < ' +  str(hi) + ' )'

	cur.execute(query)
	nbComp = cur.fetchall()
	print('>	with (' + valueToDelete + ' , ' +  valueToMaintain + ') The number of competence(s) Note(s) is  ' + str(nbComp[0][0]) )

def countWorksWithNB(hi, lo):
	global valueToDelete
	global valueToMaintain


	query ='  SELECT count(*) FROM '
	query = query +	'                        (SELECT id_devoir, count(*) as nb FROM notes.competences_devoirs '
	query = query +	'                         WHERE id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'                         group by id_devoir) as res '
	query = query +	'    WHERE res.nb < ' +  str(hi) + ' AND res.nb > ' + str(lo)

	cur.execute(query)
	nbDevoir = cur.fetchall()
	print('>	There are  ' + str(nbDevoir[0][0]) + ' devoir(s) with both ')

def countWorksWithValueToMaintainNB(hi, lo):
	global valueToDelete
	global valueToMaintain


	query ='  SELECT count(*) FROM '
	query = query +	'                        (SELECT id_devoir, count(*) as nb FROM notes.competences_devoirs '
	query = query +	'                         WHERE id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'                         group by id_devoir) as res '
	query = query +	'         INNER JOIN        notes.competences_devoirs ON competences_devoirs.id_devoir = res.id_devoir AND  id_competence = ' +  valueToMaintain
	query = query +	'    AND res.nb < ' +  str(hi) + ' AND res.nb > ' + str(lo)

	cur.execute(query)
	nbDevoir = cur.fetchall()
	print('>	There are  ' + str(nbDevoir[0][0]) + ' devoir(s) with ' + str(valueToMaintain))

def countWorksWithValueToDeleteNB(hi, lo):
	global valueToDelete
	global valueToMaintain


	query ='  SELECT count(*) FROM '
	query = query +	'                        (SELECT id_devoir, count(*) as nb FROM notes.competences_devoirs '
	query = query +	'                         WHERE id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'                         group by id_devoir) as res '
	query = query +	'         INNER JOIN        notes.competences_devoirs ON competences_devoirs.id_devoir = res.id_devoir AND  id_competence = ' +  valueToDelete
	query = query +	'    AND res.nb < ' +  str(hi) + ' AND res.nb > ' + str(lo)

	cur.execute(query)
	nbDevoir = cur.fetchall()
	print('>	There are  ' + str(nbDevoir[0][0]) + ' devoir(s) with ' + str(valueToDelete) )

def countCompetencesNotesWithValueToDeleteNB(hi, lo):
	global valueToDelete
	global valueToMaintain

	query = 'SELECT count(*) '
	query = query +	' FROM notes.competences_notes '
	query = query +	' Where (id_competence = ' + valueToDelete +  ' ) AND id_devoir IN '
	query = query +	'    (SELECT id_devoir FROM '
	query = query +	'                        (SELECT id_devoir, count(*) as nb FROM notes.competences_devoirs '
	query = query +	'                         WHERE id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'                         group by id_devoir) as res '
	query = query +	'    WHERE res.nb < ' +  str(hi) + ' AND res.nb > ' + str(lo)+ ' )'

	cur.execute(query)
	nbComp = cur.fetchall()
	print('>	with (' + valueToDelete + ') The number of competence(s) Note(s) is ' + str(nbComp[0][0]))

def countCompetencesNotesWithValueToMaintainNB(hi, lo):
	global valueToDelete
	global valueToMaintain

	query = 'SELECT count(*) '
	query = query +	' FROM notes.competences_notes '
	query = query +	' Where (id_competence = ' + valueToMaintain +  ' ) AND id_devoir IN '
	query = query +	'    (SELECT id_devoir FROM '
	query = query +	'                        (SELECT id_devoir, count(*) as nb FROM notes.competences_devoirs '
	query = query +	'                         WHERE id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'                         group by id_devoir) as res '
	query = query +	'    WHERE res.nb < ' +  str(hi) + ' AND res.nb > ' + str(lo) + ' )'

	cur.execute(query)
	nbComp = cur.fetchall()
	print('>	with (' +  valueToMaintain + ') The number of competence(s) Note(s) is ' + str(nbComp[0][0]))

def getCompNoteToSwitch():
	global valueToDelete
	global valueToMaintain
	print('(' + str(datetime.now()) + ') [Start] (getCompNoteToSwitch) CompetencesNotes must be take the max value' + str(valueToMaintain))
	query = 'SELECT * '
	query = query +	' FROM notes.competences_notes '
	query = query +	' Where (id_competence = ' +  valueToMaintain + ' ) AND id_devoir IN '
	query = query +	'    (SELECT id_devoir FROM '
	query = query +	'                        (SELECT id_devoir, count(*) as nb FROM notes.competences_devoirs '
	query = query +	'                         WHERE id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'                         group by id_devoir) as res '
	query = query +	'    WHERE res.nb > 1 ) '
	query = query +	'  ORDER BY id_devoir'

	cur.execute(query)
	print('(' + str(datetime.now()) + ') [Done] (getCompNoteToSwitch) ')


def switchCompNoteEvaluationToMax():
	global valueToDelete
	global valueToMaintain
	print('[Start] CompetencesNotes must be take the max value by student and devoir of both (' + str(valueToMaintain) + ',' + str(valueToDelete) + ')')

	query = 'WITH maxComp AS (SELECT id_devoir, id_competence, id_eleve, max (evaluation)'
	query = query +	'FROM notes.competences_notes WHERE ( id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain + ' ) '
	query = query +	'AND id_devoir IN (SELECT id_devoir FROM '
	query = query +	'               	(SELECT id_devoir, count(*) as nb '
	query = query +	'               		FROM notes.competences_devoirs '
	query = query +	'               		Where id_competence = ' + valueToDelete + ' OR id_competence = ' +  valueToMaintain
	query = query +	'               			group by id_devoir) as res '
	query = query +	'               			WHERE res.nb > 1) '
	query = query +	'               		GROUP BY (id_devoir, id_competence, id_eleve)) '
	query = query +	'  UPDATE notes.competences_notes '
	query = query +	'  SET evaluation = maxComp.max '
	query = query +	'  FROM maxComp '
	query = query +	'  WHERE ( competences_notes.id_competence = ' + valueToDelete + ' or competences_notes.id_competence = ' +  valueToMaintain + ' ) '
	query = query +	'  		AND competences_notes.id_devoir = maxComp.id_devoir AND  competences_notes.id_eleve = maxComp.id_eleve '
	query = query +	'  AND  competences_notes.id_competence = maxComp.id_competence; '
	cur.execute(query)
	nbcount = cur.rowcount
	print('[Done] ----- ' +  str(nbcount) +  " Competences Notes updated ")


def deleteCompNoteOfToDelete():
	global valueToDelete

	print('[Start] Delete all compNotes ' + str(valueToDelete))
	query = 'DELETE FROM notes.competences_notes WHERE id_competence = ' + valueToDelete
	cur.execute(query)
	nbcount = cur.rowcount
	print('[Done] ----- ' +  str(nbcount) +  " Competences Notes deleted")

def deleteCompHomeWorkOfCompToDelete():
	global valueToDelete

	print('[Start] Delete all devoirs ' + str(valueToDelete))

	query = 'DELETE FROM notes.competences_devoirs WHERE id_competence = ' + valueToDelete
	cur.execute(query)
	nbcount = cur.rowcount
	print('[Done] ----- ' +  str(nbcount) +  " competences_devoirs deleted ")

def deleteCompToDelete():
	global valueToDelete

	print('[Start] Delete The competence ' + str(valueToDelete))

	query = 'DELETE FROM notes.competences WHERE id = ' + valueToDelete
	cur.execute(query)
	nbcount = cur.rowcount
	print('[Done] ----- ' +  str(nbcount) +  " competence deleted ")

try:
	parser = argparse.ArgumentParser()
	parser.add_argument('-A','--sql_adresse',help='Adresse de la base de donnees PostgreSQL',required=True)
	parser.add_argument('-P','--sql_port',help='Numero de port de la base de donnees PostgreSQL',required=True)
	parser.add_argument('-DB','--sql_db',help='Nom de la base de donnees PostgreSQL',required=True)
	parser.add_argument('-u','--sql_user',help='Nom de compte de la base de donnees PostgreSQL',required=True)
	parser.add_argument('-srv','--srv_location',help='Chemin d acces au storage',required=True)
	group = parser.add_mutually_exclusive_group(required=True)
	group.add_argument('-pw','--sql_password',help='Nom de la base de donnees PostgreSQL')
	group.add_argument('--no-password',help='Mot de passe du compte de la base donnees PostgreSQL', action='store_true')
	args = parser.parse_args()


	print('(' + str(datetime.now()) + ')' + ' ------- CONNECT TO DATA BASE ' + '(' + str(datetime.now()) + ')')
	if args.no_password:
		sql = psycopg2.connect(database=args.sql_db, host=args.sql_adresse, user=args.sql_user)
	else:
		sql = psycopg2.connect(database=args.sql_db, host=args.sql_adresse, user=args.sql_user, password=args.sql_password)
		cur = sql.cursor()

	print('\n\n(' + str(datetime.now()) + ')' + ' ------- [START] NOT CONFLICT PART MOVE   '+ str(valueToDelete) + ' TO ' +  str(valueToMaintain))
	print('                                     Here Evaluations have only one of competences to link')
	countWorksWithNB(2,0)
	countWorksWithValueToDeleteNB(2,0)
	countCompetencesNotesWithNB(2,0)
	countCompetencesNotesWithValueToDeleteNB(2,0)
	countCompetencesNotesWithValueToMaintainNB(2,0)
	updateHomeWorkWithOnlyValueToDelete()
	sql.commit()
	updateCompetenceNoteWithOnlyValueToDelete()
	sql.commit()
	print('(' + str(datetime.now()) + ')' + ' ------- [END]   NOT CONFLICT PART  \n\n')


	print('(' + str(datetime.now()) + ')' + ' ------- [START] CONFLICT PART TAKE MAX EVALUATION OF COMPETENCES   ('+ str(valueToDelete) + ', ' +  str(valueToMaintain) + ') ')
	print('                                     Here Evaluations have both competences to link. ThereFore, we take the max of compNote by student and by devoir')
	countWorksWithNB(4,1)
	countWorksWithValueToDeleteNB(4,1)
	countWorksWithValueToMaintainNB(4,1)
	countCompetencesNotesWithNB(4,1)
	countCompetencesNotesWithValueToDeleteNB(4,1)
	countCompetencesNotesWithValueToMaintainNB(4,1)
	switchCompNoteEvaluationToMax()
	sql.commit()
	print('(' + str(datetime.now()) + ')' + ' ------- [END]  CONFLICT PART  \n\n')

	print('(' + str(datetime.now()) + ')' + ' ------- [START] NIVEAU FINAL PART   ('+ str(valueToDelete) + ') ')
	updateNiveauFinalWithOnlyValueToDelete()
	print('(' + str(datetime.now()) + ')' + ' ------- [END]  NIVEAU FINAL PART  \n\n')


	print('(' + str(datetime.now()) + ')' + ' ------- [START] DELETE PART REMOVE COMPETENCES   ('+ str(valueToDelete) + ') ')
	deleteCompNoteOfToDelete()
	sql.commit()
	deleteCompHomeWorkOfCompToDelete()
	sql.commit()
	deleteCompToDelete()
	sql.commit()
	print('(' + str(datetime.now()) + ')' + ' ------- [END]  DELETE PART  \n\n')

	closeConnection(sql, cur)
	print('            ************************************ ')
	print('            *          FINIFSH SUCCESS         * ')
	print('            ************************************ \n')

except (psycopg2.Error) as error :
	print ("Error while fetching data from PostgreSQL", error)
finally:
	closeConnection(sql, cur)