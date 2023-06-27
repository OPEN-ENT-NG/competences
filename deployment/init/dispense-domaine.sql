begin;
--domaines MN-577
UPDATE ONLY notes.domaines SET dispensable = true WHERE codification = 'D1.2' AND id_cycle = 1;
--fin domaines MN-577
commit;