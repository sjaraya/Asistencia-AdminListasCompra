package com.zamora.fastoreapp;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.zamora.fastoreapp.Adapters.AdapterProductosCompra;
import com.zamora.fastoreapp.Clases.ListaCompras;
import com.zamora.fastoreapp.Clases.Producto;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Sergio on 13/04/2017.
 */

public class ProductosListaActivity extends AppCompatActivity implements UpdateListInterface {
    final FirebaseDatabase database = FirebaseDatabase.getInstance();
    public final static ArrayList<Producto> listaProductosGlobales = new ArrayList<>();
    private AdapterProductosCompra adapter;
    String idLista;
    public static String txtSpeechInput;
    public static String nombreLista;
    public static String nombreUser;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private ArrayList<Producto> productos  = new ArrayList<>();
    public Context context;

    private double total = 0;

    private TextView lblTotal, lblLista, lblCarrito;
    private View lytDescripciones, divider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lista_productos_compra);

        Intent intent = getIntent();
        idLista = intent.getStringExtra("idLista");
        nombreLista = intent.getStringExtra("nombreLista");
        nombreUser = intent.getStringExtra("idUsuario");

        lblTotal = (TextView) findViewById(R.id.lblTotal);
        lblLista = (TextView) findViewById(R.id.lblLista);
        lblCarrito = (TextView) findViewById(R.id.lblCarrito);
        lytDescripciones = findViewById(R.id.lytDescripciones);
        divider = findViewById(R.id.divider);
        getSupportActionBar().setTitle(nombreLista);
        //listaCompras = new ListaCompras();
        //listaCompras.leer(this, nombreLista);
        //String nombre = listaCompras.getNombre();


        consultarProductos(this);
        System.out.println("Tamaño de lista: " + productos.size());


        cargarListas();
    }



    public void cargarListas() {
        ListView lv = (ListView) findViewById(R.id.productList);
        adapter = new AdapterProductosCompra(this, productos);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //int productoSeleccionado = productos.indexOf(parent.getAdapter().getItem(position));
                Producto productSelected = (Producto) parent.getAdapter().getItem(position);
                if (!productSelected.getInCart()) {
                    productSelected.setInCart(true);
                } else {
                    productSelected.setInCart(false);
                }
                adapter.notifyDataSetChanged();

                DatabaseReference refProductoCar = database.getReference("Usuarios/"+nombreUser+"/Listas/"+nombreLista+"/Detalle/"+productSelected.getNombre());
                Map<String,Object> cambio = new HashMap<String, Object>();
                cambio.put("inCart",productSelected.getInCart());
                refProductoCar.updateChildren(cambio);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_productos_lista, menu);
        MenuItem item = menu.findItem(R.id.itemSearch);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setQueryHint("Buscar...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        Intent i = getIntent();
        finish();
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemAdd:
                NuevoProductoDialog npd = new NuevoProductoDialog(ProductosListaActivity.this,nombreLista,nombreUser);
                npd.show();
                //onRestart();

                adapter.notifyDataSetChanged();
                return true;

            case R.id.itemAddByVoice:
                promptSpeechInput();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Habla ahora");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "Lo sentimos, tu dispositivo no soporta esta función",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txtSpeechInput = result.get(0);
                    String[] parse = txtSpeechInput.split(" ");
                    //Toast.makeText(this, result.get(0), Toast.LENGTH_LONG).show();
                    confirmSpeechText(result.get(0),parse);
                }
                break;
            }

        }
    }


    public AlertDialog confirmSpeechText(final String speechText,final String[] parse) {
        final String capText = speechText.substring(0, 1).toUpperCase() + speechText.substring(1);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(Html.fromHtml("<font color='#039BE5'>Agregar</font>"))
                .setMessage(capText)
                .setPositiveButton("Sí",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Producto nuevoProducto = new Producto();
                                nuevoProducto.setNombre(capText);
                                nuevoProducto.setContext(getApplicationContext());
                                ArrayList<String> numerosN = new ArrayList<String>();
                                ArrayList<String> numerosL = new ArrayList<String>();
                                numerosL.add("uno");numerosL.add("dos");numerosL.add("tres");
                                numerosL.add("cuatro");numerosL.add("cinco");numerosL.add("seis");
                                numerosL.add("siete");numerosL.add("ocho");numerosL.add("nueve");
                                numerosL.add("diez");numerosL.add("once");numerosL.add("doce");
                                numerosL.add("trece");numerosL.add("catorce");numerosL.add("quince");
                                for(int i = 0; i< 51;i++){
                                    numerosN.add(String.valueOf(i));
                                }
                                if(parse.length == 1){
                                    nuevoProducto.setCantidad(1);
                                    for (int p = 0; p < listaProductosGlobales.size(); p++) {
                                        if (parse[0].equals(listaProductosGlobales.get(p).getNombre().toLowerCase())) {
                                            //Toast.makeText(getApplicationContext(), parse[0], Toast.LENGTH_SHORT).show();
                                            nuevoProducto.setNombre(listaProductosGlobales.get(p).getNombre());
                                            nuevoProducto.setPrecio(listaProductosGlobales.get(p).getPrecio());
                                        }
                                    }
                                }
                                else {
                                    for (int i = 0; i < parse.length; i++) {
                                        //Toast.makeText(getApplicationContext(),parse[i],Toast.LENGTH_SHORT).show();
                                        for (int n = 0; n < numerosN.size(); n++) {
                                            if (parse[i].equals(numerosN.get(n))) {
                                                //Toast.makeText(getApplicationContext(), parse[i], Toast.LENGTH_SHORT).show();
                                                nuevoProducto.setCantidad(n);
                                            }
                                        }
                                        for (int nL = 1; nL < numerosL.size(); nL++) {
                                            if (parse[i].equals(numerosL.get(nL))) {
                                                //Toast.makeText(getApplicationContext(), parse[i], Toast.LENGTH_SHORT).show();
                                                nuevoProducto.setCantidad(nL);
                                            }
                                        }
                                        for (int p = 0; p < listaProductosGlobales.size(); p++) {
                                            if (parse[i].equals(listaProductosGlobales.get(p).getNombre().toLowerCase())) {
                                                //Toast.makeText(getApplicationContext(), parse[i], Toast.LENGTH_SHORT).show();
                                                nuevoProducto.setNombre(listaProductosGlobales.get(p).getNombre());
                                                nuevoProducto.setPrecio(listaProductosGlobales.get(p).getPrecio());
                                            }
                                        }
                                    }
                                }
                                nuevoProducto.insertar(nuevoProducto,nombreLista,nombreUser);
                                onRestart();
                            }
                        })
                .setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //listener.onNegativeButtonClick();
                                dialog.cancel();
                                promptSpeechInput();
                            }
                        });

        return builder.show();
    }
    /*public void opcionesElemento(final Producto selectedPro, final int posicion) {
        final CharSequence[] opciones = {"Añadido al carrito", "Eliminar"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Opciones");
        builder.setItems(opciones, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if(item == 0){

                }
                else if (item == 1) {
                    Boolean wasRemoved = productos.remove(selectedPro);
                    if (wasRemoved) {
                        DatabaseReference refEliminar = database.getReference("Usuarios/"+nombreUser+"/Listas/"+nombreLista+"/Detalle/"+selectedPro.getNombre());
                        refEliminar.removeValue();
                        Toast.makeText(getApplicationContext(), "Eiminando Producto", Toast.LENGTH_SHORT).show();

                        adapter.notifyDataSetChanged();
                    }
                }
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }*/


    public void consultarProductos(final UpdateListInterface updateListInterface) {
        final DatabaseReference refHijoUsuarioP = database.getReference("Usuarios"+"/"+ nombreUser+"/Listas");
        final ArrayList<Producto> productoArrayList = new ArrayList<>();
        refHijoUsuarioP.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                productoArrayList.clear();
                for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if(snapshot.getKey().equals(nombreLista)) {
                        for (DataSnapshot hijoList : snapshot.getChildren()) {
                            //Toast.makeText(getApplicationContext(),hijoList.getKey(),Toast.LENGTH_LONG).show();
                            if (hijoList.getKey().equals("Detalle")) {
                                for (DataSnapshot hijoD : hijoList.getChildren()) {
                                    Producto producto = hijoD.getValue(Producto.class);
                                    productoArrayList.add(producto);
                                }
                            }
                        }
                    }
                }
                updateListInterface.updateList(productoArrayList);
                //Toast.makeText(getApplicationContext(),"Cargando lista de productos",Toast.LENGTH_LONG).show();

                NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
                lblTotal.setText("Total: " + numberFormat.format(total));
                total = 0;
                if (productoArrayList.size() > 0) {
                    lytDescripciones.setVisibility(View.VISIBLE);
                    divider.setVisibility(View.VISIBLE);
                }
            }
            //+nombreLista+"/Detalle"
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        final DatabaseReference refProductos = database.getReference("Productos");
        refProductos.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaProductosGlobales.clear();
                for (DataSnapshot datosProd : dataSnapshot.getChildren()){
                    //Toast.makeText(getApplicationContext(),)
                    Producto producto = datosProd.getValue(Producto.class);
                    producto.setNombre(datosProd.getKey());
                    listaProductosGlobales.add(producto);
                }
                //Toast.makeText(getApplicationContext(),listaProductosGlobales.toString(),Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void updateList(List<Producto> productoList) {
        productos.clear();
        productos.addAll(productoList);
        int contadorPendientes = productos.size();
        int contadorCarrito = 0;
        for (int i = 0; i < productos.size(); i++) {
            double cantidadProducto = productos.get(i).getCantidad();
            double precioProducto;
            try {
                precioProducto = productos.get(i).getPrecio();
            } catch (NullPointerException ex) {
                continue;
            }

            double totalProducto = cantidadProducto * precioProducto;
            total += totalProducto;

            if (productos.get(i).getInCart()) {
                contadorCarrito++;
                contadorPendientes--;
            }

        }

        lblCarrito.setText("En carrito (" + contadorCarrito + ")");
        lblLista.setText("En lista (" + contadorPendientes + ")");
        adapter.notifyDataSetChanged();
    }



}

interface UpdateListInterface{
    void updateList(List<Producto> productoList);
}