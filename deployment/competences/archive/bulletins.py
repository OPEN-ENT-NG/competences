## python archive.py -A localdev -P 5432 -DB ong -u web-education -srv ./srv/storage -pw We_1234

import argparse
from datetime import datetime
import psycopg2
import shutil
import os

nbStructure = 0
nbClasse = 0
nbStudent = 0
nbFiles = 0
def closeConnection(sql, cur):
if(sql):
cur.close()
sql.close()
print('(' + str(datetime.now()) + ')' + " PostgreSQL connection is closed")
return

def getArchiveList():
print('(' + str(datetime.now()) + ') [Start] Get all paths')
cur.execute("""SELECT id_file, file_name FROM notes.archive_bulletins""")
print('(' + str(datetime.now()) + ') [Done] Get all paths')
return cur.fetchall()

def creatFolders(row):
global nbClasse
global nbStructure
global nbStudent
nameSplit = row[1].split('_', 3)
structure = 'archive'+ '/' + nameSplit[0]
classe = nameSplit[1]
student = nameSplit[2]
periode = nameSplit[3]

if not os.path.exists(structure):
print('Create folder Structure ' + structure)
nbStructure = nbStructure + 1
os.makedirs(structure)

if not os.path.exists(structure + '/' + classe):
print('Create folder classe ' + structure + '/' + classe)
nbClasse = nbClasse + 1
os.makedirs(structure+ '/' + classe)

if not os.path.exists(structure+ '/' + classe + '/' + student):
print('Create folder student ' + structure + '/' + classe + '/' + student)
nbStudent = nbStudent + 1
os.makedirs(structure + '/' + classe + '/' + student)

return structure + '/' + classe + '/' + student + '/' + student + periode

def getFile(id_file, srvLocation):
len_file = len(id_file)
path_file = id_file[len_file-2:len_file]
path_file = path_file + '/' + id_file[len_file-4:len_file-2]
return srvLocation + '/' + path_file + '/' + id_file

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

print('(' + str(datetime.now()) + ')' + ' ------- [START] GET BULLETINS PDF PATHS  ')
archiveList = getArchiveList()
print('(' + str(datetime.now()) + ')' + ' ------- [END]   GET BULLETINS PDF PATHS  ')

if not os.path.exists('archive'):
print('Create folder archive')
os.makedirs('archive')

print('\n(' + str(datetime.now()) + ')' + ' ------- [START] COPY FILES ')
for row in archiveList:
dest = creatFolders(row)
src = getFile(row[0], args.srv_location)
nbFiles = nbFiles + 1
try:
shutil.copy(src, dest)
except(Exception):
# print(fileError)
nbFiles = nbFiles - 1
continue

print('(' + str(datetime.now()) + ')' + ' ------- [END]   COPY FILES \n')
closeConnection(sql, cur)
print('            ************************************ ')
print('            *          END FEED BACK           * ')
print('            ************************************ \n')
print('                     ' , nbStructure , 'structures')
print('                     ' , nbClasse , 'classes')
print('                     ', nbStudent , 'students')
print('                     ', nbFiles , 'files')

except (psycopg2.Error) as error :
print ("Error while fetching data from PostgreSQL", error)
finally:
        #closing database connection.
closeConnection(sql, cur)