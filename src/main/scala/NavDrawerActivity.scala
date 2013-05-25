package org.fedoraproject.mobile

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.{ ActionBarDrawerToggle, FragmentActivity }
import android.view.View
import android.widget.{ AdapterView, ArrayAdapter }

trait NavDrawerActivity extends FragmentActivity with TypedActivity {

  val navMap = Map(
    "Infrastructure Status" -> classOf[StatusActivity],
    "Package Search" -> classOf[PackageSearchActivity]
  )

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.navdrawer)
  }

  lazy val drawerLayout = findView(TR.drawer_layout)
  lazy val drawerToggle = new ActionBarDrawerToggle(
    this,
    drawerLayout,
    R.drawable.ic_drawer,
    R.string.open,
    R.string.close) {
    override def onDrawerClosed(view: View) = invalidateOptionsMenu
    override def onDrawerOpened(view: View) = invalidateOptionsMenu
  }

  def setUpNav(layout: Int) {
    val fragment = new RootFragment(layout)
    getSupportFragmentManager
      .beginTransaction
      .replace(R.id.content_frame, fragment)
      .commit

    val drawerList = findView(TR.left_drawer)

    //drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

    getActionBar.setDisplayHomeAsUpEnabled(true);
    getActionBar.setHomeButtonEnabled(true);

    drawerList.setAdapter(
      new ArrayAdapter[String](
        this,
        R.layout.drawer_list_item,
        navMap.keys.toArray))

    drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener {
      def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
        val keys = navMap.keys.toArray
        val intent = new Intent(NavDrawerActivity.this, navMap(keys(position)))
        startActivity(intent)
      }
    })
  }

  override def onPostCreate(bundle: Bundle) {
    super.onPostCreate(bundle)
    drawerLayout.setDrawerListener(drawerToggle)
    drawerToggle.syncState
  }

  override def onConfigurationChanged(config: Configuration) {
    super.onConfigurationChanged(config)
    drawerToggle.onConfigurationChanged(config)
  }
}
