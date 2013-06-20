package au.org.intersect.faims.android.database;

public final class DatabaseQueries {

	public static final String INSERT_INTO_ARCHENTITY = 
		"INSERT INTO ArchEntity (uuid, userid, AEntTypeID, GeoSpatialColumn, AEntTimestamp) " +
			"SELECT cast(? as integer), ?, aenttypeid, GeomFromText(?, 4326), ? " +
			"FROM aenttype " + 
			"WHERE aenttypename = ? COLLATE NOCASE;";
	
	public static final String INSERT_INTO_AENTVALUE = 
		"INSERT INTO AEntValue (uuid, userid, VocabID, AttributeID, Measure, FreeText, Certainty, ValueTimestamp, deleted) " +
			"SELECT cast(? as integer), ?, ?, attributeID, ?, ?, ?, ?, ? " +
			"FROM AttributeKey " + 
			"WHERE attributeName = ? COLLATE NOCASE;";
	
	public static final String INSERT_INTO_RELATIONSHIP = 
		"INSERT INTO Relationship (RelationshipID, userid, RelnTypeID, GeoSpatialColumn, RelnTimestamp) " +
			"SELECT cast(? as integer), ?, relntypeid, GeomFromText(?, 4326), ? " +
			"FROM relntype " +
			"WHERE relntypename = ? COLLATE NOCASE;";
	
	public static final String INSERT_INTO_RELNVALUE = 
		"INSERT INTO RelnValue (RelationshipID, UserId, VocabID, AttributeID, FreeText, Certainty, RelnValueTimestamp, deleted) " +
			"SELECT cast(? as integer), ?, ?, attributeId, ?, ?, ?, ? " +
			"FROM AttributeKey " + 
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
		"INSERT INTO AEntReln (UUID, RelationshipID, UserId, ParticipatesVerb, AEntRelnTimestamp) " +
			"VALUES (?, ?, ?, ?, ?);";

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
			"          JOIN (SELECT uuid, attributeid, valuetimestamp\n" + 
			"                  FROM aentvalue\n" + 
			"                  JOIN archentity USING (uuid)\n" + 
			"                 WHERE archentity.deleted is NULL\n" + 
			"              GROUP BY uuid, attributeid\n" + 
			"                HAVING MAX(ValueTimestamp)\n" + 
			"                   AND MAX(AEntTimestamp)) USING (uuid, attributeid, valuetimestamp)\n" + 
			"          WHERE aentvalue.deleted is NULl\n" + 
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
			"    JOIN ( SELECT relationshipid, attributeid, relnvaluetimestamp, relntypeid\n" + 
			"             FROM relnvalue\n" + 
			"             JOIN relationship USING (relationshipid)\n" + 
			"            WHERE relationship.deleted is NULL\n" + 
			"         GROUP BY relationshipid, attributeid\n" + 
			"           HAVING MAX(relnvaluetimestamp)\n" + 
			"              AND MAX(relntimestamp)\n" + 
			"      ) USING (relationshipid, attributeid, relnvaluetimestamp, relntypeid)\n" + 
			"   WHERE relnvalue.deleted is NULL\n" + 
			"ORDER BY relationshipid, attributename asc)\n" + 
			"group by relationshipid;";
	}

	public static final String FETCH_ALL_VISIBLE_ENTITY_GEOMETRY(String userQuery){
		return "SELECT uuid, coalesce(group_concat(measure || vocabname), group_concat(vocabname, ', '), group_concat(measure, ', '), group_concat(freetext, ', ')) AS response, Hex(AsBinary(geospatialcolumn))\n" + 
			"    FROM (SELECT uuid, attributeid, valuetimestamp, aenttimestamp\n" + 
			"            FROM archentity\n" + 
			"            JOIN aentvalue USING (uuid)\n" + 
			"            JOIN idealaent using (aenttypeid, attributeid)\n" + 
			"           WHERE isIdentifier = 'true'\n" + 
			"             AND uuid IN (SELECT uuid\n" + 
			"                            FROM (SELECT uuid, max(aenttimestamp) as aenttimestamp, deleted as entDel\n" + 
			"                                    FROM archentity\n" + 
			"                                   where st_intersects(geospatialcolumn, PolyFromText(?, 4326))\n" + 
			"                                GROUP BY uuid, aenttypeid\n" + 
			"                                  HAVING max(aenttimestamp)\n" + 
			"                                     )\n" + 
			"                            JOIN (SELECT uuid, max(valuetimestamp) as valuetimestamp\n" + 
			"                                    FROM aentvalue --this gives us a temporal ordering...\n" + 
			"                                  WHERE deleted is null\n" + 
			"                                GROUP BY uuid\n" + 
			"                                  HAVING max(valuetimestamp)\n" + 
			"                                    )\n" + 
			"                            USING (uuid)\n" +
												userQuery +
			"                           WHERE entDel is null\n" + 
			"                           GROUP BY uuid\n" + 
			"                        ORDER BY max(valuetimestamp, aenttimestamp) desc, uuid\n" + 
			"                        LIMIT ?\n" + 
			"                        -- OFFSET ?\n" + 
			"                      )\n" + 
			"        GROUP BY uuid, attributeid\n" + 
			"          HAVING MAX(ValueTimestamp)\n" + 
			"             AND MAX(AEntTimestamp)\n" + 
			"             )\n" + 
			"    JOIN attributekey using (attributeid)\n" + 
			"    JOIN aentvalue using (uuid, attributeid, valuetimestamp)\n" + 
			"    JOIN (SELECT uuid, max(valuetimestamp) AS tstamp FROM aentvalue GROUP BY uuid) USING (uuid)\n" + 
			"    JOIN (SELECT uuid, max(aenttimestamp) AS astamp FROM archentity GROUP BY uuid) USING (uuid)\n" + 
			"    JOIN archentity using (uuid, aenttimestamp)\n" + 
			"    JOIN aenttype using (aenttypeid)\n" + 
			"    LEFT OUTER JOIN vocabulary USING (vocabid, attributeid)\n" + 
			"WHERE aentvalue.deleted is null\n" + 
			"group by uuid\n" + 
			"ORDER BY max(tstamp,astamp) desc, uuid, attributename;";
	}

	public static final String FETCH_ALL_VISIBLE_RELN_GEOMETRY(String userQuery){
		return "SELECT relationshipid, coalesce(group_concat(vocabname, ', '), group_concat(freetext, ', ')) as response, Hex(AsBinary(geospatialcolumn))\n" + 
			"   FROM ( SELECT relationshipid, attributeid, relntimestamp, relnvaluetimestamp\n" + 
			"            FROM relationship\n" + 
			"            JOIN relnvalue USING (relationshipid)\n" + 
			"            JOIN idealreln using (relntypeid, attributeid)\n" + 
			"           WHERE isIdentifier = 'true'\n" + 
			"             AND relationshipid in (SELECT distinct relationshipid\n" + 
			"                                      FROM (SELECT relationshipid, max(relntimestamp) as relntimestamp, deleted as relnDeleted\n" + 
			"                                              FROM relationship\n" + 
			"                                              where st_intersects(geospatialcolumn, PolyFromText(?, 4326))\n" + 
			"                                          GROUP BY relationshipid\n" + 
			"                                            HAVING max(relntimestamp))\n" + 
			"                                      JOIN (SELECT relationshipid, attributeid, max(relnvaluetimestamp) as relnvaluetimestamp\n" + 
			"                                              FROM relnvalue\n" + 
			"                                             WHERE deleted is null\n" + 
			"                                          GROUP BY relationshipid, attributeid, vocabid\n" + 
			"                                            HAVING max(relnvaluetimestamp)\n" + 
			"                                        ) USING (relationshipid)\n" + 
														userQuery +
			"                                     WHERE relnDeleted is null\n" + 
			"                                  GROUP BY relationshipid\n" + 
			"                                  ORDER BY max(relnvaluetimestamp, relntimestamp) desc, relationshipid\n" + 
			"                                  LIMIT ?\n" + 
			"                                  --OFFSET ?\n" + 
			"                                    )\n" + 
			"        GROUP BY relationshipid, attributeid\n" + 
			"          HAVING MAX(relntimestamp)\n" + 
			"             AND MAX(relnvaluetimestamp))\n" + 
			"   JOIN relationship using (relationshipid, relntimestamp)\n" + 
			"   JOIN relntype using (relntypeid)\n" + 
			"   JOIN attributekey using (attributeid)\n" + 
			"   JOIN relnvalue using (relationshipid, relnvaluetimestamp, attributeid)\n" + 
			"   LEFT OUTER JOIN vocabulary using (vocabid, attributeid)\n" + 
			"   JOIN (SELECT relationshipid, max(relnvaluetimestamp) AS tstamp FROM relnvalue GROUP BY relationshipid) USING (relationshipid)\n" + 
			"   JOIN (SELECT relationshipid, max(relntimestamp) AS astamp FROM relationship GROUP BY relationshipid) USING (relationshipid)\n" + 
			"  WHERE relnvalue.deleted is NULL\n" + 
			"GROUP BY relationshipid, attributeid, relnvaluetimestamp\n" + 
			"ORDER BY max(tstamp,astamp) desc, relationshipid, attributename;";
	}

	public static final String COUNT_ENTITY_TYPE =
		"select count(AEntTypeID) from AEntType where AEntTypeName = ? COLLATE NOCASE;";

	public static final String COUNT_ENTITY =
		"select count(UUID) from ArchEntity where UUID = ?;";

	public static final String COUNT_RELN_TYPE =
		"select count(RelnTypeID) from RelnType where RelnTypeName = ? COLLATE NOCASE;";

	public static final String COUNT_RELN =
		"select count(RelationshipID) from Relationship where RelationshipID = ?;";

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
			" and st_intersects(buffer(transform(GeomFromText(?, 4326), 3785), ?), transform(geospatialcolumn,3785))";

	public static String RUN_DISTANCE_RELATIONSHIP =
		"select relationshipid, relntimestamp\n" + 
			" from (select relationshipid, max(relntimestamp) as relntimestamp, deleted, geospatialcolumn\n" + 
			"          from relationship \n" + 
			"      group by relationshipid \n" + 
			"        having max(relntimestamp))\n" + 
			" where deleted is null\n" +
			" and geospatialcolumn is not null\n" +
			" and st_intersects(buffer(transform(GeomFromText(?, 4326), 3785), ?), transform(geospatialcolumn,3785))";
}
