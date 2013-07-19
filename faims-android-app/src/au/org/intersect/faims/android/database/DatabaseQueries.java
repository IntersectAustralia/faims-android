package au.org.intersect.faims.android.database;

public final class DatabaseQueries {

	public static final String INSERT_INTO_ARCHENTITY = 
		"INSERT INTO ArchEntity (uuid, userid, AEntTypeID, GeoSpatialColumn, AEntTimestamp, parenttimestamp) " +
			"SELECT cast(? as integer), ?, aenttypeid, GeomFromText(?, 4326), ?, parent.aenttimestamp " +
			"FROM aenttype " +
			"LEFT OUTER JOIN (\n" + 
			"SELECT aenttimestamp\n" + 
			"FROM archentity\n" + 
			"JOIN\n" + 
			"  (SELECT uuid,\n" + 
			"          max(aenttimestamp) AS aenttimestamp\n" + 
			"   FROM archentity\n" + 
			"   GROUP BY uuid) USING (uuid,\n" + 
			"                         aenttimestamp)\n" + 
			"WHERE uuid = ?) parent\n" +
			"WHERE aenttypename = ? COLLATE NOCASE;";
	
	public static final String INSERT_AND_UPDATE_INTO_ARCHENTITY = 
			"INSERT INTO ArchEntity (uuid, userid, AEntTypeID, GeoSpatialColumn, parenttimestamp)\n" + 
			"SELECT uuid, ?, aenttypeid, GeomFromText(?, 4326), parent.aenttimestamp\n" + 
			"FROM (SELECT uuid, aenttypeid FROM archentity where uuid = ? group by uuid) " +
			"LEFT OUTER JOIN (\n" + 
			"SELECT aenttimestamp\n" + 
			"FROM archentity\n" + 
			"JOIN\n" + 
			"  (SELECT uuid,\n" + 
			"          max(aenttimestamp) AS aenttimestamp\n" + 
			"   FROM archentity\n" + 
			"   GROUP BY uuid) USING (uuid,\n" + 
			"                         aenttimestamp)\n" + 
			"WHERE uuid = ?) parent;";

	public static final String GET_ARCH_ENT_PARENT_TIMESTAMP =
		"SELECT max(aenttimestamp) FROM archentity WHERE uuid = ? group by uuid;";

	public static final String INSERT_INTO_AENTVALUE = 
		"INSERT INTO AEntValue (uuid, userid, VocabID, AttributeID, Measure, FreeText, Certainty, ValueTimestamp, deleted, parenttimestamp) " +
			"SELECT cast(? as integer), ?, ?, attributeID, ?, ?, ?, ?, ?, parent.valuetimestamp " +
			"FROM AttributeKey " +
			"LEFT OUTER JOIN (\n" + 
			"SELECT valuetimestamp\n" + 
			"FROM aentvalue\n" +
			"JOIN attributekey using (attributeid)\n" +
			"JOIN\n" + 
			"  (SELECT uuid,\n" + 
			"          attributeid,\n" + 
			"          MAX(valuetimestamp) AS valuetimestamp\n" + 
			"   FROM aentvalue\n" + 
			"   GROUP BY uuid,\n" + 
			"            attributeid) USING (uuid,\n" + 
			"                                attributeid,\n" + 
			"                                valuetimestamp)\n" + 
			"WHERE uuid = ?\n" + 
			"\n" + 
			"  AND attributeName = ? COLLATE NOCASE) parent\n" +
			"WHERE attributeName = ? COLLATE NOCASE;";

	public static final String GET_AENT_VALUE_PARENT_TIMESTAMP =
		"SELECT attributename, max(valuetimestamp) FROM aentvalue " +
			"JOIN attributekey using (attributeid) " +
			"WHERE uuid = ? group by attributeid;";

	public static final String INSERT_INTO_RELATIONSHIP = 
		"INSERT INTO Relationship (RelationshipID, userid, RelnTypeID, GeoSpatialColumn, RelnTimestamp, parenttimestamp) " +
			"SELECT cast(? as integer), ?, relntypeid, GeomFromText(?, 4326), ?, parent.relntimestamp " +
			"FROM relntype " +
			"LEFT OUTER JOIN (\n" + 
			"SELECT relntimestamp\n" + 
			"FROM relationship\n" + 
			"JOIN\n" + 
			"  (SELECT relationshipid,\n" + 
			"          max(relntimestamp) AS relntimestamp\n" + 
			"   FROM relationship\n" + 
			"   GROUP BY relationshipid) USING (relationshipid,\n" + 
			"                                   relntimestamp)\n" + 
			"WHERE relationshipid = ?) parent\n" +
			"WHERE relntypename = ? COLLATE NOCASE;";
	
	public static final String INSERT_AND_UPDATE_INTO_RELATIONSHIP = 
			"INSERT INTO Relationship (relationshipid, userid, RelnTypeID, GeoSpatialColumn, parenttimestamp)\n" + 
			"SELECT relationshipid, ?, relntypeid, GeomFromText(?, 4326), parent.relntimestamp\n" + 
			"FROM (SELECT relationshipid, relntypeid FROM relationship where relationshipid = ? group by relationshipid) " +
			"LEFT OUTER JOIN (\n" + 
			"SELECT relntimestamp\n" + 
			"FROM relationship\n" + 
			"JOIN\n" + 
			"  (SELECT relationshipid,\n" + 
			"          max(relntimestamp) AS relntimestamp\n" + 
			"   FROM relationship\n" + 
			"   GROUP BY relationshipid) USING (relationshipid,\n" + 
			"                                   relntimestamp)\n" + 
			"WHERE relationshipid = ?) parent;";
	
	public static final String GET_RELATIONSHIP_PARENT_TIMESTAMP =
		"SELECT max(relntimestamp) FROM relationship WHERE relationshipid = ? group by relationshipid;";

	public static final String GET_RELN_VALUE_PARENT_TIMESTAMP =
		"SELECT attributename, max(relnvaluetimestamp) FROM relnvalue " +
			"JOIN attributekey using (attributeid) " +
			"WHERE relationshipid = ? group by attributeid;";

	public static final String INSERT_INTO_RELNVALUE = 
		"INSERT INTO RelnValue (RelationshipID, UserId, VocabID, AttributeID, FreeText, Certainty, RelnValueTimestamp, deleted, parenttimestamp) " +
			"SELECT cast(? as integer), ?, ?, attributeId, ?, ?, ?, ?, parent.relnvaluetimestamp " +
			"FROM AttributeKey " +
			"LEFT OUTER JOIN (\n" + 
			"SELECT relnvaluetimestamp\n" + 
			"FROM relnvalue\n" + 
			"JOIN attributekey using (attributeid)\n" +
			"JOIN\n" + 
			"  (SELECT relationshipid, attributeid, MAX(relnvaluetimestamp) AS relnvaluetimestamp\n" + 
			"   FROM relnvalue\n" + 
			"   GROUP BY relationshipid,\n" + 
			"            attributeid) USING (relationshipid,\n" + 
			"                                attributeid,\n" + 
			"                                relnvaluetimestamp)\n" + 
			"WHERE relationshipid = ?\n" + 
			"\n" + 
			"  AND attributeName = ? COLLATE NOCASE) parent\n" +
			"WHERE attributeName = ? COLLATE NOCASE;";

	public static final String CHECK_VALID_AENT = 
		"SELECT count(AEntTypeName) " + 
			"FROM IdealAEnt left outer join AEntType using (AEntTypeId) left outer join AttributeKey using (AttributeId) " + 
			"WHERE AEntTypeName = ? COLLATE NOCASE and AttributeName = ? COLLATE NOCASE;";

	public static final String CHECK_VALID_RELN = 
		"SELECT count(RelnTypeName) " + 
			"FROM IdealReln left outer join RelnType using (RelnTypeID) left outer join AttributeKey using (AttributeId) " + 
			"WHERE RelnTypeName = ? COLLATE NOCASE and AttributeName = ? COLLATE NOCASE;";

	public static final String INSERT_AENT_RELN = 
		"INSERT INTO AEntReln (UUID, RelationshipID, UserId, ParticipatesVerb, AEntRelnTimestamp, parenttimestamp) " +
			"SELECT ?, ?, ?, ?, ?, parent.aentrelntimestamp\n" +
			"FROM (SELECT 1) LEFT OUTER JOIN (\n" + 
			"SELECT aentrelntimestamp\n" + 
			"FROM aentreln\n" + 
			"JOIN\n" + 
			"  (SELECT uuid,\n" + 
			"          relationshipid,\n" + 
			"          max(AEntRelnTimestamp) AS AEntRelnTimestamp\n" + 
			"   FROM aentreln\n" + 
			"   GROUP BY uuid,\n" + 
			"            relationshipid) USING (uuid,\n" + 
			"                                   relationshipid,\n" + 
			"                                   AEntRelnTimestamp)\n" + 
			"WHERE uuid = ?\n" + 
			"  AND relationshipid = ?) parent";

	public static final String FETCH_AENT_VALUE = 
		"SELECT uuid, attributename, vocabid, measure, freetext, certainty, attributetype, aentvaluedeleted, aentdirty, aentdirtyreason FROM " +
			"(SELECT uuid, attributeid, vocabid, measure, freetext, certainty, valuetimestamp, aentvalue.deleted as aentvaluedeleted, aentvalue.isDirty as aentdirty, aentvalue.isDirtyReason as aentdirtyreason FROM aentvalue WHERE uuid || valuetimestamp || attributeid in " +
				"(SELECT uuid || max(valuetimestamp) || attributeid FROM aentvalue WHERE uuid = ? GROUP BY uuid, attributeid) ) " +
			"JOIN attributekey USING (attributeid) " +
			"JOIN ArchEntity USING (uuid) " +
			"where uuid || aenttimestamp in ( select uuid || max(aenttimestamp) from archentity group by uuid having deleted is null);";

	public static final String FETCH_ARCHENTITY_GEOSPATIALCOLUMN = 
		"SELECT uuid, HEX(AsBinary(GeoSpatialColumn)) from ArchEntity where uuid || aenttimestamp IN" +
				"( SELECT uuid || max(aenttimestamp) FROM archentity WHERE uuid = ?);";

	public static final String FETCH_RELN_VALUE = 
		"SELECT relationshipid, attributename, vocabid, freetext, certainty, attributetype, relnvaluedeleted, relndirty, relndirtyreason FROM " +
			"(SELECT relationshipid, attributeid, vocabid, freetext, certainty, relnvalue.deleted as relnvaluedeleted, relnvalue.isDirty as relndirty, relnvalue.isDirtyReason as relndirtyreason FROM relnvalue WHERE relationshipid || relnvaluetimestamp || attributeid in " +
				"(SELECT relationshipid || max(relnvaluetimestamp) || attributeid FROM relnvalue WHERE relationshipid = ? GROUP BY relationshipid, attributeid having deleted is null)) " +
			"JOIN attributekey USING (attributeid) " +
			"JOIN Relationship USING (relationshipid) " +
			"where relationshipid || relntimestamp in (select relationshipid || max (relntimestamp) from relationship group by relationshipid having deleted is null )";

	public static final String FETCH_RELN_GEOSPATIALCOLUMN =
		"SELECT relationshipid, HEX(AsBinary(GeoSpatialColumn)) from relationship where relationshipid || relntimestamp IN" +
			"( SELECT relationshipid || max(relntimestamp) FROM relationship WHERE relationshipid = ?);";

	public static final String FETCH_ENTITY_LIST(String type){
		return "select uuid, group_concat(coalesce(measure    || ' '  || vocabname  || '(' ||freetext||'; '|| (certainty * 100.0) || '% certain)',\n" +
			"                                                                                              measure    || ' (' || freetext   ||'; '|| (certainty * 100.0)  || '% certain)',\n" +
			"                                                                                              vocabname  || ' (' || freetext   ||'; '|| (certainty * 100.0)  || '% certain)',\n" +
			"                                                                                              measure    || ' ' || vocabname   ||' ('|| (certainty * 100.0)  || '% certain)',\n" +
			"                                                                                              vocabname  || ' (' || freetext || ')',\n" +
			"                                                                                              measure    || ' (' || freetext || ')',\n" +
			"                                                                                              measure    || ' (' || (certainty * 100.0) || '% certain)',\n" +
			"                                                                                              vocabname  || ' (' || (certainty * 100.0) || '% certain)',\n" +
			"                                                                                              freetext   || ' (' || (certainty * 100.0) || '% certain)',\n" +
			"                                                                                              measure,\n" +
			"                                                                                              vocabname,\n" +
			"                                                                                              freetext), ' | ') as response\n" +
			"FROM (  SELECT uuid, attributeid, vocabid, attributename, vocabname, measure, freetext, certainty, attributetype, valuetimestamp\n" +
			"          FROM aentvalue\n" +
			"          JOIN attributekey USING (attributeid)\n" +
			"          join archentity USING (uuid)\n" +
			"          join (select attributeid, aenttypeid from idealaent join aenttype using (aenttypeid) where isIdentifier is 'true' and lower(aenttypename) = lower('" + type + "')) USING (attributeid, aenttypeid)\n" +
			"          LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n" +
			"          JOIN (SELECT uuid, attributeid, max(valuetimestamp) as valuetimestamp, max(aenttimestamp) as aenttimestamp\n" +
			"                  FROM aentvalue\n" +
			"                  JOIN archentity USING (uuid)\n" +
			"              GROUP BY uuid, attributeid\n" +
			"                ) USING (uuid, attributeid, valuetimestamp, aenttimestamp)\n" +
			"          WHERE aentvalue.deleted is NULl\n" +
			"          and archentity.deleted is NULL\n" +
			"       ORDER BY uuid, attributename ASC)\n" +
			"group by uuid;";
	}

	public static final String FETCH_RELN_LIST(String type){
		return "select relationshipid, group_concat(coalesce(vocabname  || ' (' || freetext   ||'; '|| (certainty * 100.0)  || '% certain)',\n" +
			"                                                                                         vocabname  || ' (' || freetext || ')',\n" +
			"                                                                                         vocabname  || ' (' || (certainty * 100.0) || '% certain)',\n" +
			"                                                                                         freetext   || ' (' || (certainty * 100.0) || '% certain)',\n" +
			"                                                                                         vocabname,\n" +
			"                                                                                         freetext), ' | ') as response\n" +
			"from (\n" +
			"SELECT relationshipid, vocabid, attributeid, attributename, freetext, certainty, vocabname, relntypeid, attributetype, relnvaluetimestamp\n" +
			"    FROM relnvalue\n" +
			"    JOIN attributekey USING (attributeid)\n" +
			"    JOIN relationship USING (relationshipid)\n" +
			"    join  (select attributeid, relntypeid from idealreln join relntype using (relntypeid) where isIdentifier is 'true' and lower(relntypename) = lower('" + type + "')) USING (attributeid, relntypeid)\n" +
			"    LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n" +
			"    JOIN ( SELECT relationshipid, attributeid, max(relnvaluetimestamp) as relnvaluetimestamp, max(relntimestamp) as relntimestamp, relntypeid\n" +
			"             FROM relnvalue\n" +
			"             JOIN relationship USING (relationshipid)\n" +
			"         GROUP BY relationshipid, attributeid\n" +
			"      ) USING (relationshipid, attributeid, relnvaluetimestamp, relntimestamp, relntypeid)\n" +
			"   WHERE relnvalue.deleted is NULL\n" +
			"   and relationship.deleted is NULL\n" +
			"ORDER BY relationshipid, attributename asc)\n" +
			"group by relationshipid;";
	}

	public static final String FETCH_ALL_VISIBLE_ENTITY_GEOMETRY(String userQuery){
		return "select uuid, group_concat(coalesce(measure    || ' '  || vocabname  || '(' ||freetext||'; '|| (certainty * 100.0) || '% certain)',\n" +
			"                                      measure    || ' (' || freetext   ||'; '|| (certainty * 100.0)  || '% certain)',\n"+
			"                                      vocabname  || ' (' || freetext   ||'; '|| (certainty * 100.0)  || '% certain)',\n"+
			"                                      measure    || ' ' || vocabname   ||' ('|| (certainty * 100.0)  || '% certain)',\n"+
			"                                      vocabname  || ' (' || freetext || ')',\n"+
			"                                      measure    || ' (' || freetext || ')',\n"+
			"                                      measure    || ' (' || (certainty * 100.0) || '% certain)',\n"+
			"                                      vocabname  || ' (' || (certainty * 100.0) || '% certain)',\n"+
			"                                      freetext   || ' (' || (certainty * 100.0) || '% certain)',\n"+
			"                                      measure,\n"+
			"                                      vocabname,\n"+
			"                                      freetext), ' | ') as response, hex(asbinary(geospatialcolumn))\n"+
			"FROM (  SELECT uuid, geospatialcolumn, attributeid, vocabid, attributename, vocabname, measure, freetext, certainty, attributetype, valuetimestamp\n"+
			"          FROM aentvalue\n"+
			"          JOIN attributekey USING (attributeid)\n"+
			"          join archentity USING (uuid)\n"+
			"          join (select attributeid, aenttypeid from idealaent join aenttype using (aenttypeid) where isIdentifier is 'true') USING (attributeid, aenttypeid)\n"+
			"          LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n"+
			"          JOIN (SELECT uuid, attributeid, max(valuetimestamp) as valuetimestamp, max(aenttimestamp) as aenttimestamp\n"+
			"                  FROM aentvalue\n"+
			"                  JOIN archentity USING (uuid)\n"+
			"              GROUP BY uuid, attributeid\n"+
			"                ) USING (uuid, attributeid, valuetimestamp, aenttimestamp)\n"+
			"          WHERE aentvalue.deleted is NULL\n"+
			"          and archentity.deleted is NULL\n"+
			"          and st_intersects(transform(geospatialcolumn, 4326), PolyFromText(?, 4326))\n"+
			"       ORDER BY uuid, attributename ASC, valuetimestamp desc)\n"+
			"group by uuid limit ?;";
	}
	
	public static final String COUNT_ALL_VISIBLE_ENTITY_GEOMETRY(String userQuery){
		return "select count(uuid)\n"+
			"FROM (  SELECT uuid, geospatialcolumn, attributeid, vocabid, attributename, vocabname, measure, freetext, certainty, attributetype, valuetimestamp\n"+
			"          FROM aentvalue\n"+
			"          JOIN attributekey USING (attributeid)\n"+
			"          join archentity USING (uuid)\n"+
			"          join (select attributeid, aenttypeid from idealaent join aenttype using (aenttypeid) where isIdentifier is 'true') USING (attributeid, aenttypeid)\n"+
			"          LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n"+
			"          JOIN (SELECT uuid, attributeid, max(valuetimestamp) as valuetimestamp, max(aenttimestamp) as aenttimestamp\n"+
			"                  FROM aentvalue\n"+
			"                  JOIN archentity USING (uuid)\n"+
			"              GROUP BY uuid, attributeid\n"+
			"                ) USING (uuid, attributeid, valuetimestamp, aenttimestamp)\n"+
			"          WHERE aentvalue.deleted is NULL\n"+
			"          and archentity.deleted is NULL\n"+
			"          and st_intersects(transform(geospatialcolumn, 4326), PolyFromText(?, 4326))\n"+
			"       ORDER BY uuid, attributename ASC, valuetimestamp desc)\n"+
			"group by uuid;";
	}

	public static final String FETCH_ALL_VISIBLE_RELN_GEOMETRY(String userQuery){
		return "select relationshipid, group_concat(coalesce(vocabname  || ' (' || freetext   ||'; '|| (certainty * 100.0)  || '% certain)',\n"+
			"                                      vocabname  || ' (' || freetext || ')',\n"+
			"                                      vocabname  || ' (' || (certainty * 100.0) || '% certain)',\n"+
			"                                      freetext   || ' (' || (certainty * 100.0) || '% certain)',\n"+
			"                                      vocabname,\n"+
			"                                      freetext), ' | ') as response, Hex(AsBinary(geospatialcolumn))\n"+
			"      from (\n"+
			"      SELECT relationshipid, geospatialcolumn, vocabid, attributeid, attributename, freetext, certainty, vocabname, relntypeid, attributetype, relnvaluetimestamp\n"+
			"          FROM relnvalue\n"+
			"          JOIN attributekey USING (attributeid)\n"+
			"          JOIN relationship USING (relationshipid)\n"+
			"          join  (select attributeid, relntypeid from idealreln join relntype using (relntypeid) where isIdentifier is 'true') USING (attributeid, relntypeid)\n"+
			"          LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n"+
			"          JOIN ( SELECT relationshipid, attributeid, max(relnvaluetimestamp) as relnvaluetimestamp, max(relntimestamp) as relntimestamp, relntypeid\n"+
			"                   FROM relnvalue\n"+
			"                   JOIN relationship USING (relationshipid)\n"+

			"               GROUP BY relationshipid, attributeid\n"+
			"            ) USING (relationshipid, attributeid, relnvaluetimestamp, relntimestamp, relntypeid)\n"+
			"         WHERE relnvalue.deleted is NULL\n"+
			"         and relationship.deleted is NULL\n"+
			"                    and st_intersects(transform(geospatialcolumn, 4326), PolyFromText(?, 4326))\n"+
			"      ORDER BY relationshipid, attributename asc)\n"+
			"      group by relationshipid limit ?;";
	}
	
	public static final String COUNT_ALL_VISIBLE_RELN_GEOMETRY(String userQuery){
		return "select count(relationshipid)\n"+
			"      from (\n"+
			"      SELECT relationshipid, geospatialcolumn, vocabid, attributeid, attributename, freetext, certainty, vocabname, relntypeid, attributetype, relnvaluetimestamp\n"+
			"          FROM relnvalue\n"+
			"          JOIN attributekey USING (attributeid)\n"+
			"          JOIN relationship USING (relationshipid)\n"+
			"          join  (select attributeid, relntypeid from idealreln join relntype using (relntypeid) where isIdentifier is 'true') USING (attributeid, relntypeid)\n"+
			"          LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n"+
			"          JOIN ( SELECT relationshipid, attributeid, max(relnvaluetimestamp) as relnvaluetimestamp, max(relntimestamp) as relntimestamp, relntypeid\n"+
			"                   FROM relnvalue\n"+
			"                   JOIN relationship USING (relationshipid)\n"+

			"               GROUP BY relationshipid, attributeid\n"+
			"            ) USING (relationshipid, attributeid, relnvaluetimestamp, relntimestamp, relntypeid)\n"+
			"         WHERE relnvalue.deleted is NULL\n"+
			"         and relationship.deleted is NULL\n"+
			"                    and st_intersects(transform(geospatialcolumn, 4326), PolyFromText(?, 4326))\n"+
			"      ORDER BY relationshipid, attributename asc)\n"+
			"      group by relationshipid;";
	}

	public static final String COUNT_ENTITY_TYPE =
		"select count(AEntTypeID) from AEntType where AEntTypeName = ? COLLATE NOCASE;";

	public static final String COUNT_ENTITY =
		"select count(UUID) from ArchEntity where UUID = ?;";

	public static final String COUNT_RELN_TYPE =
		"select count(RelnTypeID) from RelnType where RelnTypeName = ? COLLATE NOCASE;";

	public static final String COUNT_RELN =
		"select count(RelationshipID) from Relationship where RelationshipID = ?;";

	public static final String DELETE_ARCH_ENT =
		"insert into archentity (uuid, userid, AEntTypeID, GeoSpatialColumnType, GeoSpatialColumn, deleted, parenttimestamp) "+
			"select uuid, ? , AEntTypeID, GeoSpatialColumnType, GeoSpatialColumn, 'true', parent.aenttimestamp " +
			"from (select uuid, max(aenttimestamp) as aenttimestamp " +
			"from archentity "+
			"where uuid = ? "+
			"group by uuid) "+
			"JOIN archentity using (uuid, aenttimestamp) "+
			"LEFT OUTER JOIN (\n" + 
			"SELECT aenttimestamp\n" + 
			"FROM archentity\n" + 
			"JOIN\n" + 
			"  (SELECT uuid,\n" + 
			"          max(aenttimestamp) AS aenttimestamp\n" + 
			"   FROM archentity\n" + 
			"   GROUP BY uuid) USING (uuid,\n" + 
			"                         aenttimestamp)\n" + 
			"WHERE uuid = ?) parent;";

	public static final String DELETE_RELN =
		"insert into relationship (RelationshipID, userid, RelnTypeID, GeoSpatialColumnType, GeoSpatialColumn, deleted, parenttimestamp) "+
			"select RelationshipID, ?, RelnTypeID, GeoSpatialColumnType, GeoSpatialColumn, 'true', parent.relntimestamp " +
			"from (select relationshipid, max(relntimestamp) as RelnTimestamp "+
			"FROM relationship " +
			"where relationshipID = ? "+
			"group by relationshipid "+
			") JOIN relationship using (relationshipid, relntimestamp) "+
			"LEFT OUTER JOIN (\n" + 
			"SELECT relntimestamp\n" + 
			"FROM relationship\n" + 
			"JOIN\n" + 
			"  (SELECT relationshipid,\n" + 
			"          max(relntimestamp) AS relntimestamp\n" + 
			"   FROM relationship\n" + 
			"   GROUP BY relationshipid) USING (relationshipid,\n" + 
			"                                   relntimestamp)\n" + 
			"WHERE relationshipid = ?) parent;";

	public static final String DUMP_DATABASE_TO(String path){
		return "attach database '" + path + "' as export;" +
				"create table export.archentity as select * from archentity;" +
				"create table export.aentvalue as select * from aentvalue;" +
				"create table export.aentreln as select * from aentreln;" + 
				"create table export.relationship as select * from relationship;" +
				"create table export.relnvalue as select * from relnvalue;" +
				"detach database export;";
	}

	public static final String DUMP_DATABASE_TO(String path, String fromTimestamp){
		return "attach database '" + path + "' as export;" +
				"create table export.archentity as select * from archentity where aenttimestamp >= '" + fromTimestamp + "';" +
				"create table export.aentvalue as select * from aentvalue where valuetimestamp >= '" + fromTimestamp + "';" +
				"create table export.aentreln as select * from aentreln where aentrelntimestamp >= '" + fromTimestamp + "';" +
				"create table export.relationship as select * from relationship where relntimestamp >= '" + fromTimestamp + "';" +
				"create table export.relnvalue as select * from relnvalue where relnvaluetimestamp >= '" + fromTimestamp + "';" +
				"detach database export;";
	}

	public static final String MERGE_DATABASE_FROM(String path){
		return "attach database '" + path + "' as import;" +
				"insert or replace into archentity (\n" + 
				"         uuid, aenttimestamp, userid, doi, aenttypeid, deleted, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn) \n" + 
				"  select uuid, aenttimestamp, userid, doi, aenttypeid, deleted, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn \n" + 
				"  from import.archentity;\n" +
				"delete from aentvalue\n" + 
				"    where uuid || valuetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')|| coalesce(measure, '')|| coalesce(certainty, '')|| userid IN\n" + 
				"    (select uuid || valuetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')|| coalesce(measure, '')|| coalesce(certainty, '')|| userid from import.aentvalue);" +
				"insert into aentvalue (\n" + 
				"         uuid, valuetimestamp, userid, attributeid, vocabid, freetext, measure, certainty, deleted, isdirty, isdirtyreason, isforked, parenttimestamp) \n" + 
				"  select uuid, valuetimestamp, userid, attributeid, vocabid, freetext, measure, certainty, deleted, isdirty, isdirtyreason, isforked, parenttimestamp \n" + 
				"  from import.aentvalue where uuid || valuetimestamp || attributeid not in (select uuid || valuetimestamp||attributeid from aentvalue);\n" + 
				"insert or replace into relationship (\n" + 
				"         relationshipid, userid, relntimestamp, relntypeid, deleted, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn) \n" + 
				"  select relationshipid, userid, relntimestamp, relntypeid, deleted, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn\n" + 
				"  from import.relationship;\n" + 
				"delete from relnvalue\n" + 
				"    where relationshipid || relnvaluetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')||  coalesce(certainty, '')|| userid IN\n" + 
				"    (select relationshipid || relnvaluetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')|| coalesce(certainty, '')|| userid from import.relnvalue);" +
				"insert into relnvalue (\n" + 
				"         relationshipid, relnvaluetimestamp, userid, attributeid, vocabid, freetext, certainty, deleted, isdirty, isdirtyreason, isforked, parenttimestamp) \n" + 
				"  select relationshipid, relnvaluetimestamp, userid, attributeid, vocabid, freetext, certainty, deleted, isdirty, isdirtyreason, isforked, parenttimestamp \n" + 
				"  from import.relnvalue where relationshipid || relnvaluetimestamp || attributeid not in (select relationshipid || relnvaluetimestamp || attributeid from relnvalue);\n" + 
				"insert into aentreln (\n" + 
				"         uuid, relationshipid, userid, aentrelntimestamp, participatesverb, deleted, isdirty, isdirtyreason, isforked, parenttimestamp) \n" + 
				"  select uuid, relationshipid, userid, aentrelntimestamp, participatesverb, deleted, isdirty, isdirtyreason, isforked, parenttimestamp\n" + 
				"  from import.aentreln where uuid || relationshipid || aentrelntimestamp not in (select uuid || relationshipid || aentrelntimestamp from aentreln);\n" + 
				"insert or replace into vocabulary (\n" + 
				"         vocabid, attributeid, vocabname, SemanticMapURL,PictureURL) \n" + 
				"  select vocabid, attributeid, vocabname, SemanticMapURL,PictureURL\n" + 
				"  from import.vocabulary;\n" + 
				"insert or replace into user (\n" + 
				"         userid, fname, lname) \n" + 
				"  select userid, fname, lname\n" + 
				"  from import.user;\n" + 
				"detach database import;";
	}

	public static String RUN_DISTANCE_ENTITY = 
		"select uuid, aenttimestamp\n" + 
			" from (select uuid, max(aenttimestamp) as aenttimestamp, deleted, geospatialcolumn\n" + 
			"          from archentity \n" + 
			"      group by uuid \n" + 
			"        having max(aenttimestamp))\n" + 
			" where deleted is null\n" +
			" and geospatialcolumn is not null\n" +
			" and st_intersects(buffer(transform(GeomFromText(?, 4326), ?), ?), transform(geospatialcolumn, ?))";

	public static String RUN_DISTANCE_RELATIONSHIP =
		"select relationshipid, relntimestamp\n" + 
			" from (select relationshipid, max(relntimestamp) as relntimestamp, deleted, geospatialcolumn\n" + 
			"          from relationship \n" + 
			"      group by relationshipid \n" + 
			"        having max(relntimestamp))\n" + 
			" where deleted is null\n" +
			" and geospatialcolumn is not null\n" +
			" and st_intersects(buffer(transform(GeomFromText(?, 4326), ?), ?), transform(geospatialcolumn, ?))";

	public static String RUN_INTERSECT_ENTITY = 
			"select uuid, aenttimestamp\n" + 
				" from (select uuid, max(aenttimestamp) as aenttimestamp, deleted, geospatialcolumn\n" + 
				"          from archentity \n" + 
				"      group by uuid \n" + 
				"        having max(aenttimestamp))\n" + 
				" where deleted is null\n" +
				" and geospatialcolumn is not null\n" +
				" and st_intersects(GeomFromText(?, 4326), geospatialcolumn)";

	public static String RUN_INTERSECT_RELATIONSHIP =
			"select relationshipid, relntimestamp\n" + 
				" from (select relationshipid, max(relntimestamp) as relntimestamp, deleted, geospatialcolumn\n" + 
				"          from relationship \n" + 
				"      group by relationshipid \n" + 
				"        having max(relntimestamp))\n" + 
				" where deleted is null\n" +
				" and geospatialcolumn is not null\n" +
				" and st_intersects(GeomFromText(?, 4326), geospatialcolumn)";
}
