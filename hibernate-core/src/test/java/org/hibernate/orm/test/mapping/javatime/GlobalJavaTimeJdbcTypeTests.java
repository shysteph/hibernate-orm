/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.mapping.javatime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.descriptor.DateTimeUtils.roundToDefaultPrecision;

/**
 * Tests for "direct" JDBC handling of {@linkplain java.time Java Time} types.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(
		settings = @Setting(name = MappingSettings.PREFER_JAVA_TYPE_JDBC_TYPES, value = "true")
)
@DomainModel( annotatedClasses = GlobalJavaTimeJdbcTypeTests.EntityWithJavaTimeValues.class )
@SessionFactory
@SkipForDialect( dialectClass = SybaseDialect.class, reason = "Sybase drivers do not comply with JDBC 4.2 requirements for support of Java Time objects", matchSubTypes = true )
@SkipForDialect( dialectClass = DB2Dialect.class, reason = "DB2 drivers do not comply with JDBC 4.2 requirements for support of Java Time objects", matchSubTypes = true )
@SkipForDialect( dialectClass = DerbyDialect.class, reason = "Derby drivers do not comply with JDBC 4.2 requirements for support of Java Time objects" )
public class GlobalJavaTimeJdbcTypeTests {
	@Test
	void testMappings(DomainModelScope scope) {
		final PersistentClass entityBinding = scope.getEntityBinding( EntityWithJavaTimeValues.class );

		checkAttribute( entityBinding, "theLocalDate", LocalDateJdbcType.class );
		checkAttribute( entityBinding, "theLocalDateTime", LocalDateTimeJdbcType.class );
		checkAttribute( entityBinding, "theLocalTime", LocalTimeJdbcType.class );
	}

	private void checkAttribute(
			PersistentClass entityBinding,
			String attributeName,
			Class<? extends JavaTimeJdbcType> expectedJdbcTypeDescriptorType) {
		final Property property = entityBinding.getProperty( attributeName );
		final BasicValue value = (BasicValue) property.getValue();
		final BasicValue.Resolution<?> resolution = value.resolve();
		final JdbcType jdbcType = resolution.getJdbcType();
		assertThat( jdbcType ).isInstanceOf( expectedJdbcTypeDescriptorType );
	}

	@Test
	void testInstant(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Instant start = roundToDefaultPrecision( Instant.EPOCH, dialect );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = new EntityWithJavaTimeValues();
			entity.id = 1;
			entity.theInstant = start;
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theInstant ).isEqualTo( start );
			entity.theInstant = start.plus( 2000, ChronoUnit.DAYS );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theInstant ).isEqualTo( start.plus( 2000, ChronoUnit.DAYS ) );
			entity.theInstant = start.minus( 2000, ChronoUnit.DAYS );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theInstant ).isEqualTo( start.minus( 2000, ChronoUnit.DAYS ) );
		} );
	}

	@Test
	void testLocalDateTime(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final LocalDateTime start = roundToDefaultPrecision( LocalDateTime.now(), dialect );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = new EntityWithJavaTimeValues();
			entity.id = 1;
			entity.theLocalDateTime = start;
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theLocalDateTime ).isEqualTo( start );
			entity.theLocalDateTime = start.plusDays( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theLocalDateTime ).isEqualTo( start.plusDays( 2000 ) );
			entity.theLocalDateTime = start.minusDays( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theLocalDateTime ).isEqualTo( start.minusDays( 2000 ) );
		} );
	}

	@Test
	void testLocalDate(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final LocalDate startTime = roundToDefaultPrecision( LocalDate.now(), dialect );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = new EntityWithJavaTimeValues();
			entity.id = 1;
			entity.theLocalDate = startTime;
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theLocalDate ).isEqualTo( startTime );
			entity.theLocalDate = startTime.plusDays( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theLocalDate ).isEqualTo( startTime.plusDays( 2000 ) );
			entity.theLocalDate = startTime.minusDays( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theLocalDate ).isEqualTo( startTime.minusDays( 2000 ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle drivers truncate fractional seconds from the LocalTime", matchSubTypes = true)
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA time type does not support fractional seconds", matchSubTypes = true)
	void testLocalTime(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final LocalTime startTime = roundToDefaultPrecision( LocalTime.now(), dialect );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = new EntityWithJavaTimeValues();
			entity.id = 1;
			entity.theLocalTime = startTime;
			session.persist( entity );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theLocalTime ).isEqualTo( startTime );
			entity.theLocalTime = startTime.plusHours( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theLocalTime ).isEqualTo( startTime.plusHours( 2000 ) );
			entity.theLocalTime = startTime.plusHours( 2000 );
		} );

		scope.inTransaction( (session) -> {
			final EntityWithJavaTimeValues entity = session.get( EntityWithJavaTimeValues.class, 1 );
			assertThat( entity.theLocalTime ).isEqualTo( startTime.plusHours( 2000 ) );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete EntityWithJavaTimeValues" ).executeUpdate();
		} );
	}

	@Entity(name="EntityWithJavaTimeValues")
	@Table(name="EntityWithJavaTimeValues")
	public static class EntityWithJavaTimeValues {
		@Id
		private Integer id;
		private String name;

		private Instant theInstant;

		private LocalDateTime theLocalDateTime;

		private LocalDate theLocalDate;

		private LocalTime theLocalTime;
	}
}
