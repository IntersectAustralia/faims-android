package au.org.intersect.faims.android.database;

public final class DatabaseQueries {

	public static final String INSERT_INTO_ARCHENTITY = 
		"INSERT INTO ArchEntity (uuid, userid, AEntTypeID, GeoSpatialColumn, AEntTimestamp, parenttimestamp) " +
			"SELECT cast(? as integer), ?, aenttypeid, GeomFromText(?, 4326), ?, ? " +
			"FROM aenttype " +
			"WHERE aenttypename = ?;";
	
	public static final String INSERT_AND_UPDATE_INTO_ARCHENTITY = 
			"INSERT INTO ArchEntity (uuid, userid, AEntTypeID, GeoSpatialColumn, parenttimestamp)\n" + 
			"SELECT uuid, ?, aenttypeid, GeomFromText(?, 4326), ? " + 
			"FROM (SELECT uuid, aenttypeid FROM archentity where uuid = ? group by uuid);";

	public static final String GET_ARCH_ENT_PARENT_TIMESTAMP =
		"SELECT max(aenttimestamp) FROM archentity WHERE uuid = ? group by uuid;";
	
	public static final String GET_AENTVALUE_PARENT_TIMESTAMP =
		"SELECT max(valuetimestamp) FROM aentvalue JOIN attributekey using (attributeid) WHERE uuid = ? and attributename = ? group by uuid, attributeid;";

	public static final String INSERT_INTO_AENTVALUE = 
		"INSERT INTO AEntValue (uuid, userid, VocabID, AttributeID, Measure, FreeText, Certainty, ValueTimestamp, deleted, parenttimestamp) " +
			"SELECT cast(? as integer), ?, ?, attributeID, ?, ?, ?, ?, ?, ? " +
			"FROM AttributeKey " +
			"WHERE attributeName = ?;";

	public static final String INSERT_INTO_RELATIONSHIP = 
		"INSERT INTO Relationship (RelationshipID, userid, RelnTypeID, GeoSpatialColumn, RelnTimestamp, parenttimestamp) " +
			"SELECT cast(? as integer), ?, relntypeid, GeomFromText(?, 4326), ?, ? " +
			"FROM relntype " +
			"WHERE relntypename = ?;";
	
	public static final String INSERT_AND_UPDATE_INTO_RELATIONSHIP = 
			"INSERT INTO Relationship (relationshipid, userid, RelnTypeID, GeoSpatialColumn, parenttimestamp)\n" + 
			"SELECT relationshipid, ?, relntypeid, GeomFromText(?, 4326), ? " + 
			"FROM (SELECT relationshipid, relntypeid FROM relationship where relationshipid = ? group by relationshipid);";
	
	public static final String GET_RELATIONSHIP_PARENT_TIMESTAMP =
		"SELECT max(relntimestamp) FROM relationship WHERE relationshipid = ? group by relationshipid;";
	
	public static final String GET_RELNVALUE_PARENT_TIMESTAMP =
			"SELECT max(relnvaluetimestamp) FROM relnvalue JOIN attributekey using (attributeid) WHERE relationshipid = ? and attributename = ? group by relationshipid, attributeid;";
	
	public static final String INSERT_INTO_RELNVALUE = 
		"INSERT INTO RelnValue (RelationshipID, UserId, VocabID, AttributeID, FreeText, Certainty, RelnValueTimestamp, deleted, parenttimestamp) " +
			"SELECT cast(? as integer), ?, ?, attributeId, ?, ?, ?, ?, ? " +
			"FROM AttributeKey " +
			"WHERE attributeName = ?;";

	public static final String CHECK_VALID_AENT = 
		"SELECT count(AEntTypeName) " + 
			"FROM IdealAEnt left outer join AEntType using (AEntTypeId) left outer join AttributeKey using (AttributeId) " + 
			"WHERE AEntTypeName = ? and AttributeName = ?;";

	public static final String CHECK_VALID_RELN = 
		"SELECT count(RelnTypeName) " + 
			"FROM IdealReln left outer join RelnType using (RelnTypeID) left outer join AttributeKey using (AttributeId) " + 
			"WHERE RelnTypeName = ? and AttributeName = ?;";
	
	public static final String GET_AENT_RELN_PARENT_TIMESTAMP =
		"SELECT max(aentrelntimestamp) from aentreln where uuid = ? and relationshipid = ? group by uuid, relationshipid;";

	public static final String INSERT_AENT_RELN = 
		"INSERT INTO AEntReln (UUID, RelationshipID, UserId, ParticipatesVerb, AEntRelnTimestamp, parenttimestamp) " +
			"VALUES (?, ?, ?, ?, ?, ?);";

	public static final String FETCH_AENT_VALUE =
		"SELECT uuid, attributename, vocabid, measure, freetext, certainty, attributetype, av.deleted, av.isdirty, av.isdirtyreason, attributeisfile, attributeusethumbnail \n" + 
		"FROM latestNonDeletedArchent JOIN latestNonDeletedAentvalue AS av using (uuid) JOIN attributekey using (attributeid) \n" + 
		"WHERE uuid = ?;";
		
	public static final String FETCH_ARCHENTITY_GEOSPATIALCOLUMN = 
		"SELECT uuid, HEX(asBinary(geospatialColumn)) FROM latestNonDeletedArchent WHERE uuid = ?;";
	
	public static final String FETCH_RELN_VALUE = 
		"SELECT relationshipid, attributename, vocabid, freetext, certainty, attributetype, rv.deleted, rv.isdirty, rv.isdirtyreason, attributeisfile, attributeusethumbnail \n" + 
		"FROM latestNonDeletedRelationship JOIN latestNonDeletedRelnvalue AS rv using (relationshipid) JOIN attributekey using (attributeid) \n" + 
		"WHERE relationshipid = ?;";

	public static final String FETCH_RELN_GEOSPATIALCOLUMN =
		"SELECT relationshipid, HEX(asBinary(geospatialColumn)) FROM latestNonDeletedRelationship WHERE relationshipid = ?;";

	public static final String FETCH_ENTITY_LIST(String type) {
		return 
				"select uuid, group_concat(response, ' ')\n" + 
				"from (select uuid, aenttimestamp\n" + 
				"		from latestnondeletedarchent join aenttype using(aenttypeid)\n" + 
				"	   where aenttypename='" + type + "'\n" + 
				"	   order by uuid) \n" + 
				"join latestNonDeletedArchEntFormattedIdentifiers using (uuid)\n" + 
				"join createdModifiedAtBy using (uuid)    \n" + 
				"group by uuid\n" + 
				"order by createdAt\n" + 
				";";
	}

	public static final String FETCH_RELN_LIST(String type){
		return "SELECT relationshipid, group_concat(coalesce(vocabname || ' (' || freetext  ||'; '|| (certainty * 100.0) || '% certain)', \n" + 
				" 													 vocabname || ' (' || freetext || ')', \n" + 
				" 												 	 vocabname || ' (' || (certainty * 100.0) || '% certain)', \n" + 
				" 													 freetext  || ' (' || (certainty * 100.0) || '% certain)', \n" + 
				" 													 vocabname, \n" + 
				" 													 freetext), ' | ') as response \n" + 
				" 		FROM latestNonDeletedRelnIdentifiers \n" + 
				"		WHERE relntypename = '" + type + "'\n" + 
				" 		GROUP BY relationshipid;";
	}

	public static final String FETCH_ALL_VISIBLE_ENTITY_GEOMETRY(String userQuery){
		return
			"select uuid, group_concat(response, ' '), hex(asbinary(geospatialcolumn)\n" + 
			"  from latestnondeletedarchentidientifiers join latestnondeletedarchent using (uuid)\n" + 
			"  WHERE arowid in (SELECT pkid\n" + 
			"                     FROM idx_archentity_geospatialcolumn\n" + 
			"                    WHERE pkid MATCH RTreeIntersects(?, ?, ?, ?))\n" + 
			"                 ORDER BY uuid, attributename ASC, valuetimestamp desc)\n" + 
			"                 GROUP BY uuid limit ?;";
	}

	public static final String GET_BOUNDARY_OF_ALL_VISIBLE_ENTITY_GEOMETRY(String userQuery){
		return
			"SELECT hex(asBinary(extent(geospatialcolumn)))  \n" +
			"FROM ( SELECT uuid, geospatialcolumn, attributeid, vocabid, attributename, vocabname, measure, freetext, certainty, attributetype, valuetimestamp  \n" +
			"     FROM latestNonDeletedArchentIdentifiers  \n" +
				userQuery +
			"    ORDER BY uuid, attributename ASC, valuetimestamp desc);";
	}

	public static final String FETCH_ALL_VISIBLE_RELN_GEOMETRY(String userQuery){
		return
			"SELECT relationshipid, group_concat(coalesce(vocabname || ' (' || freetext  ||'; '|| (certainty * 100.0) || '% certain)',  \n" +
			"                   vocabname || ' (' || freetext || ')',  \n" +
			"                   vocabname || ' (' || (certainty * 100.0) || '% certain)',  \n" +
			"                   freetext  || ' (' || (certainty * 100.0) || '% certain)',  \n" +
			"                   vocabname,  \n" +
			"                   freetext), ' | ') as response, Hex(AsBinary(geospatialcolumn))  \n" +
			"   FROM (  \n" +
			"   SELECT relationshipid, geospatialcolumn, vocabid, attributeid, attributename, freetext, certainty, vocabname, attributetype, relnvaluetimestamp, relationship.rowid as rrowid  \n" +
			"     FROM latestNonDeletedRelnIdentifiers  join relationship using (relationshipid, relntimestamp, geospatialcolumn)\n" +
 				userQuery +
			"     WHERE  \n" +
			"     rrowid in (SELECT pkid FROM idx_relationship_geospatialcolumn WHERE pkid MATCH RtreeIntersects(?, ?, ?, ?))  \n" +
			"   ORDER BY relationshipid, attributename asc)  \n" +
			"   GROUP BY relationshipid limit ?;";
	}
	
	public static final String GET_BOUNDARY_OF_ALL_VISIBLE_RELN_GEOMETRY(String userQuery){
		return
			"SELECT Hex(AsBinary(extent(geospatialcolumn)))  \n" +
			"   FROM (  \n" +
			"   SELECT relationshipid, geospatialcolumn, vocabid, attributeid, attributename, freetext, certainty, vocabname, relntypeid, attributetype, relnvaluetimestamp  \n" +
			"     FROM latestNonDeletedRelnIdentifiers  \n" +
 					userQuery +
			"   ORDER BY relationshipid, attributename asc);";
	}
	
	public static final String COUNT_ENTITY_TYPE =
		"select count(AEntTypeID) from AEntType where AEntTypeName = ?;";

	public static final String COUNT_ENTITY =
		"select count(UUID) from ArchEntity where UUID = ?;";

	public static final String COUNT_RELN_TYPE =
		"select count(RelnTypeID) from RelnType where RelnTypeName = ?;";

	public static final String COUNT_RELN =
		"select count(RelationshipID) from Relationship where RelationshipID = ?;";

	public static final String DELETE_ARCH_ENT =
		"insert into archentity (uuid, userid, AEntTypeID, GeoSpatialColumnType, GeoSpatialColumn, deleted, parenttimestamp) \n" +
			"             SELECT uuid, ? ,     AEntTypeID, GeoSpatialColumnType, GeoSpatialColumn, 'true', ? \n" +
			"               FROM latestNonDeletedArchent\n" +
			"               where uuid = ?;";

	public static final String DELETE_RELN =
		"insert into relationship (RelationshipID, userid, RelnTypeID, GeoSpatialColumnType, GeoSpatialColumn, deleted, parenttimestamp) \n" +
		"	SELECT RelationshipID, ?, RelnTypeID, GeoSpatialColumnType, GeoSpatialColumn, 'true', ?  \n" +
		"	FROM latestNonDeletedRelationship\n" +
		"				where relationshipid = ?;";

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
				"create table export.archentity as select * from archentity where versionnum is null and aenttimestamp >= '" + fromTimestamp + "';" +
				"create table export.aentvalue as select * from aentvalue where versionnum is null and valuetimestamp >= '" + fromTimestamp + "';" +
				"create table export.aentreln as select * from aentreln where versionnum is null and aentrelntimestamp >= '" + fromTimestamp + "';" +
				"create table export.relationship as select * from relationship where versionnum is null and relntimestamp >= '" + fromTimestamp + "';" +
				"create table export.relnvalue as select * from relnvalue where versionnum is null and relnvaluetimestamp >= '" + fromTimestamp + "';" +
				"detach database export;";
	}

	public static final String MERGE_DATABASE_FROM(String path){
		return "attach database '" + path + "' as import;" +
				"insert or replace into archentity (\n" + 
				"         uuid, aenttimestamp, userid, doi, aenttypeid, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn) \n" + 
				"  select uuid, aenttimestamp, userid, doi, aenttypeid, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn \n" + 
				"  from import.archentity;\n" +
				"delete from aentvalue\n" + 
				"    where uuid || valuetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')|| coalesce(measure, '')|| coalesce(certainty, '')|| userid IN\n" + 
				"    (select uuid || valuetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')|| coalesce(measure, '')|| coalesce(certainty, '')|| userid from import.aentvalue);" +
				"insert into aentvalue (\n" + 
				"         uuid, valuetimestamp, userid, attributeid, vocabid, freetext, measure, certainty, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp) \n" + 
				"  select uuid, valuetimestamp, userid, attributeid, vocabid, freetext, measure, certainty, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp \n" + 
				"  from import.aentvalue where uuid || valuetimestamp || attributeid not in (select uuid || valuetimestamp||attributeid from aentvalue);\n" + 
				"insert or replace into relationship (\n" + 
				"         relationshipid, userid, relntimestamp, relntypeid, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn) \n" + 
				"  select relationshipid, userid, relntimestamp, relntypeid, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp, geospatialcolumntype, geospatialcolumn\n" + 
				"  from import.relationship;\n" + 
				"delete from relnvalue\n" + 
				"    where relationshipid || relnvaluetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')||  coalesce(certainty, '')|| userid IN\n" + 
				"    (select relationshipid || relnvaluetimestamp || attributeid || coalesce(vocabid, '')|| coalesce(freetext, '')|| coalesce(certainty, '')|| userid from import.relnvalue);" +
				"insert into relnvalue (\n" + 
				"         relationshipid, relnvaluetimestamp, userid, attributeid, vocabid, freetext, certainty, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp) \n" + 
				"  select relationshipid, relnvaluetimestamp, userid, attributeid, vocabid, freetext, certainty, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp \n" + 
				"  from import.relnvalue where relationshipid || relnvaluetimestamp || attributeid not in (select relationshipid || relnvaluetimestamp || attributeid from relnvalue);\n" + 
				"insert into aentreln (\n" + 
				"         uuid, relationshipid, userid, aentrelntimestamp, participatesverb, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp) \n" + 
				"  select uuid, relationshipid, userid, aentrelntimestamp, participatesverb, deleted, versionnum, isdirty, isdirtyreason, isforked, parenttimestamp\n" + 
				"  from import.aentreln where uuid || relationshipid || aentrelntimestamp not in (select uuid || relationshipid || aentrelntimestamp from aentreln);\n" + 
				"insert or replace into vocabulary (\n" + 
				"         vocabid, attributeid, vocabname, vocabdescription, VocabCountOrder, VocabDeleted, parentvocabid, SemanticMapURL, PictureURL) \n" + 
				"  select vocabid, attributeid, vocabname, vocabdescription, VocabCountOrder, VocabDeleted, parentvocabid, SemanticMapURL, PictureURL\n" + 
				"  from import.vocabulary;\n" + 
				"insert or replace into user (\n" + 
				"         userid, fname, lname, email, UserDeleted) \n" + 
				"  select userid, fname, lname, email, UserDeleted\n" + 
				"  from import.user;\n" + 
				"insert or replace into File (Filename, MD5Checksum, Size, Type, State, Timestamp, Deleted, ThumbnailFilename, ThumbnailMD5Checksum, ThumbnailSize)\n" +
				"  select Filename, MD5Checksum, Size, Type, import.file.State, Timestamp, Deleted, ThumbnailFilename, ThumbnailMD5Checksum, ThumbnailSize\n" +
				"  from import.file left outer join (select filename, state from file) local using (filename) where local.State != 'downloaded' or local.state is null;" + 
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
	
	public static String IS_ARCH_ENTITY_FORKED = 
			"select count(isforked) from archentity where uuid = ?;";
	
	public static String IS_AENTVALUE_FORKED = 
			"select count(isforked) from aentvalue where uuid = ?;";
	
	public static String IS_RELATIONSHIP_FORKED = 
			"select count(isforked) from relationship where relationshipid = ?;";
	
	public static String IS_RELNVALUE_FORKED = 
			"select count(isforked) from relnvalue where relationshipid = ?;";
	
	public static String GET_ATTRIBUTE_DESCRIPTION =
			"select attributedescription from attributekey where attributename = ?;";
	
	public static final String GET_VOCABULARIES_TERM_DESCRIPTION =
			"select vocabid, vocabname, vocabdescription, pictureurl, parentvocabid from attributekey join vocabulary using (attributeid) where attributename = ? order by VocabCountOrder;";

	public static String COUNT_AENT_RECORDS(String timestamp) {
		return "select count(*) from archentity where versionnum is null and aenttimestamp >= '" + timestamp + "';";
	}
	
	public static String COUNT_AENT_VALUE_RECORDS(String timestamp) {
		return "select count(*) from aentvalue where versionnum is null and valuetimestamp >= '" + timestamp + "';";
	}

	public static String COUNT_RELN_RECORDS(String timestamp) {
		return "select count(*) from relationship where versionnum is null and relntimestamp >= '" + timestamp + "';";
	}
	
	public static String COUNT_RELN_VALUE_RECORDS(String timestamp) {
		return "select count(*) from relnvalue where versionnum is null and relnvaluetimestamp >= '" + timestamp + "';";
	}
	
	public static String COUNT_AENT_RELN_RECORDS(String timestamp) {
		return "select count(*) from aentreln where versionnum is null and aentrelntimestamp >= '" + timestamp + "';";
	}
	
	public static String INSERT_FILE = "insert or replace into File (Filename, MD5Checksum, Size, Type, State, Timestamp, Deleted, ThumbnailFilename, ThumbnailMD5Checksum, ThumbnailSize) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
	
	public static String UPDATE_FILE = "update File set State = ? where Filename = ?";
	
	public static String GET_UPLOAD_FILES = "select Filename, MD5Checksum, Size, Type, State, Timestamp, Deleted, ThumbnailFilename, ThumbnailMD5Checksum, ThumbnailSize from File where Type = ? and State = 'attached';";
	
	public static String GET_DOWNLOAD_FILES = "select Filename, MD5Checksum, Size, Type, State, Timestamp, Deleted, ThumbnailFilename, ThumbnailMD5Checksum, ThumbnailSize from File where Type = ? and State is null;";
	
	public static String HAS_FILE_CHANGES = "select count(*) from file where state = 'attached';";
	
	public static String HAS_THUMBNAIL = "select count(*) from attributekey where attributeisfile = 1 and attributeusethumbnail = 1 and attributename = ?";
}